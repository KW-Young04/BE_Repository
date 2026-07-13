package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.IssueLocation;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.example.be_young04.domain.snapshot.dto.PageSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AnalysisPromptBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PromptBuildResult build(
            String repositoryUrl,
            List<PageSnapshot> snapshots,
            List<MatchedTarget> matchedTargets,
            List<WcagCheckResult> fallbackTargets,
            Map<String, String> fileContents
    ) {
        Map<String, Integer> snapshotIdToImageIndex = new LinkedHashMap<>();
        for (int i = 0; i < snapshots.size(); i++) {
            snapshotIdToImageIndex.put(snapshots.get(i).snapshotId(), i + 1);
        }

        List<PromptLocation> promptLocations = new ArrayList<>();
        int seq = 1;

        for (MatchedTarget target : matchedTargets) {
            List<Integer> imageIndexes = target.snapshotIds().stream()
                    .map(snapshotIdToImageIndex::get)
                    .filter(Objects::nonNull)
                    .toList();

            seq = assignLocationIds(target.result(), imageIndexes, promptLocations, seq);
        }

        for (WcagCheckResult result : fallbackTargets) {
            seq = assignLocationIds(result, List.of(), promptLocations, seq);
        }

        Map<String, String> relevantFileContents = new LinkedHashMap<>();
        for (PromptLocation pl : promptLocations) {
            String path = pl.result().getFilePath();
            if (path != null && fileContents.containsKey(path)) {
                relevantFileContents.putIfAbsent(path, fileContents.get(path));
            }
        }

        String prompt = buildPromptText(repositoryUrl, snapshots, promptLocations, relevantFileContents);

        return new PromptBuildResult(prompt, promptLocations);
    }

    private int assignLocationIds(
            WcagCheckResult result,
            List<Integer> imageIndexes,
            List<PromptLocation> out,
            int seq
    ) {
        List<IssueLocation> locations = result.getLocations();

        if (locations == null || locations.isEmpty()) {
            out.add(new PromptLocation("L" + seq++, result, null, imageIndexes));
            return seq;
        }

        for (IssueLocation location : locations) {
            out.add(new PromptLocation("L" + seq++, result, location, imageIndexes));
        }
        return seq;
    }

    private String buildPromptText(
            String repositoryUrl,
            List<PageSnapshot> snapshots,
            List<PromptLocation> promptLocations,
            Map<String, String> fileContents
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                당신은 웹 접근성(WCAG 2.2) 전문가입니다.
                아래 저장소의 프론트엔드 코드와 스크린샷을 분석하여 각 위치(locationId)별로 위반 여부를 판단해주세요.
                반드시 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                [저장소 URL]
                %s

                """.formatted(repositoryUrl));

        sb.append("[첨부 이미지 목록]\n");
        if (snapshots.isEmpty()) {
            sb.append("- 첨부된 이미지 없음\n\n");
        } else {
            for (int i = 0; i < snapshots.size(); i++) {
                PageSnapshot snapshot = snapshots.get(i);
                sb.append("이미지 %d번 — 렌더링 파일: %s\n"
                        .formatted(i + 1, String.join(", ", snapshot.renderedFilePaths())));
            }
            sb.append("\n");
        }

        sb.append("[관련 파일 코드]\n");
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            sb.append("파일명: ").append(entry.getKey()).append("\n");
            sb.append("```\n").append(truncate(entry.getValue())).append("\n```\n\n");
        }

        sb.append("[판단 요청 항목]\n");
        for (PromptLocation pl : promptLocations) {
            WcagCheckResult result = pl.result();
            IssueLocation location = pl.location();

            String imageRef = pl.imageIndexes().isEmpty()
                    ? "없음 (이미지 없이 코드만으로 판단)"
                    : pl.imageIndexes().stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("없음");

            sb.append("""
                    - locationId: %s
                      wcagId: %s / 판단유형: %s
                      대상 파일: %s
                      참고 이미지: %s
                      cssSelector: %s
                      위반 의심 코드: %s
                      판단 근거/컨텍스트: %s
                    """.formatted(
                    pl.locationId(),
                    result.getWcagId(),
                    result.getJudgeType(),
                    result.getFilePath(),
                    imageRef,
                    location != null ? String.valueOf(location.getCssSelector()) : "-",
                    location != null ? String.valueOf(location.getViolatedCode()) : "-",
                    result.getAiContext()
            ));
        }

        sb.append("""

                [응답 형식 - JSON만 출력]
                {
                  "results": [
                    {
                      "locationId": "L1",
                      "violated": true,
                      "suggestion": "구체적인 수정 방법 (violated가 false면 null)"
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    /**
     * AI 응답을 locationId 기준으로 파싱하여, 각 위치별 판단 결과로 되돌린다.
     * - 파싱 자체가 실패하면 전체를 판단 실패(violated=null)로 처리
     * - 개별 locationId가 응답에서 누락되면 그 위치만 판단 실패로 처리 (나머지는 정상 반영)
     * - 응답에 존재하지만 원본에 없는 locationId(AI 환각)는 무시
     */
    public List<LocationJudgement> parseAiResponse(String aiResponse, List<PromptLocation> promptLocations) {
        Map<String, PromptLocation> byId = promptLocations.stream()
                .collect(Collectors.toMap(PromptLocation::locationId, Function.identity()));

        try {
            String clean = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode root = objectMapper.readTree(clean);
            JsonNode resultsNode = root.get("results");

            if (resultsNode == null || !resultsNode.isArray()) {
                return fallbackJudgements(promptLocations);
            }

            List<LocationJudgement> judgements = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();

            for (JsonNode node : resultsNode) {
                String locationId = node.path("locationId").asText(null);
                PromptLocation pl = (locationId != null) ? byId.get(locationId) : null;
                if (pl == null) continue; // 원본에 없는 locationId는 무시

                seenIds.add(locationId);

                Boolean violated = (node.hasNonNull("violated"))
                        ? node.get("violated").asBoolean()
                        : null;
                String suggestion = node.hasNonNull("suggestion")
                        ? node.get("suggestion").asText(null)
                        : null;

                judgements.add(new LocationJudgement(pl, violated, suggestion));
            }

            // 응답에서 누락된 locationId는 판단 실패로 처리 (violated=null)
            for (PromptLocation pl : promptLocations) {
                if (!seenIds.contains(pl.locationId())) {
                    judgements.add(new LocationJudgement(pl, null, null));
                }
            }

            return judgements;

        } catch (Exception e) {
            return fallbackJudgements(promptLocations);
        }
    }

    private List<LocationJudgement> fallbackJudgements(List<PromptLocation> promptLocations) {
        return promptLocations.stream()
                .map(pl -> new LocationJudgement(pl, null, null))
                .toList();
    }

    private String truncate(String content) {
        if (content == null) return "";
        return content.length() > 3000
                ? content.substring(0, 3000) + "\n...[생략]"
                : content;
    }

    public record PromptLocation(
            String locationId,
            WcagCheckResult result,
            IssueLocation location,
            List<Integer> imageIndexes
    ) {
    }

    public record PromptBuildResult(
            String prompt,
            List<PromptLocation> locations
    ) {
    }

    /**
     * locationId 하나에 대한 AI의 최종 판단.
     * location이 null이면 위치 특정 없이 result(파일) 전체에 대한 판단이었다는 뜻.
     */
    public record LocationJudgement(
            PromptLocation promptLocation,
            Boolean violated,
            String suggestion
    ) {
    }
}