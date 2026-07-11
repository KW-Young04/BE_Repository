package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.IssueLocation;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class AnalysisPromptBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String build(
            String repositoryUrl,
            boolean hasSnapshot,
            Map<String, String> fileContents,
            List<WcagCheckResult> codeAiResults,
            List<WcagCheckResult> aiResults
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                당신은 웹 접근성(WCAG 2.2) 전문가입니다.
                아래 저장소의 프론트엔드 코드를 분석하여 각 WCAG 항목의 위반 여부를 판단해주세요.
                반드시 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                [저장소 URL]
                %s

                [스냅샷 바이트]
                %s

                """.formatted(
                        repositoryUrl,
                        hasSnapshot ? "[이미지 첨부됨 - 시각적 항목 판단에 활용하세요]" : "[스냅샷 없음 - 코드만으로 판단하세요]"
                ));

        sb.append("[관련 파일 코드]\n");
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            sb.append("파일명: ").append(entry.getKey()).append("\n");
            sb.append("```\n").append(truncate(entry.getValue())).append("\n```\n\n");
        }

        sb.append("[판단 요청 항목]\n");

        for (WcagCheckResult result : codeAiResults) {
            sb.append("""
                    - wcagId: %s
                      판단유형: CODE_AI (코드 분석 결과 있음, 추가 판단 필요)
                      코드 분석 결과: %s
                      추가 판단 요청: %s
                    """.formatted(
                    result.getWcagId(),
                    result.getMessage(),
                    result.getAiContext()
            ));
        }

        for (WcagCheckResult result : aiResults) {
            sb.append("""
                    - wcagId: %s
                      판단유형: AI (AI 전적 판단)
                      판단 요청: %s
                    """.formatted(
                    result.getWcagId(),
                    result.getAiContext()
            ));
        }

        sb.append("""

                [응답 형식 - JSON만 출력]
                {
                  "results": [
                    {
                      "wcagId": "1.1.1",
                      "violated": true,
                      "filePath": "위반 파일 경로 (없으면 null)",
                      "violatedCode": "위반된 코드 스니펫 (없으면 null)",
                      "cssSelector": "CSS 선택자 (없으면 null)",
                      "message": "위반 이유 또는 판단 근거",
                      "suggestion": "구체적인 수정 방법"
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    public List<WcagCheckResult> parseAiResponse(
            String aiResponse,
            List<WcagCheckResult> codeAiResults,
            List<WcagCheckResult> aiResults
    ) {
        List<WcagCheckResult> parsed = new ArrayList<>();

        try {
            String clean = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode root = objectMapper.readTree(clean);
            JsonNode resultsNode = root.get("results");

            if (resultsNode == null || !resultsNode.isArray()) {
                return fallback(codeAiResults, aiResults);
            }

            for (JsonNode node : resultsNode) {
                String wcagId = node.path("wcagId").asText();
                WcagCheckResult original = findOriginal(wcagId, codeAiResults, aiResults);

                JudgeType judgeType = original != null ? original.getJudgeType() : JudgeType.AI;
                Long wcagItemId = original != null ? original.getWcagItemId() : null;
                String title = original != null ? original.getTitle() : null;

                IssueLocation location = IssueLocation.builder()
                        .cssSelector(node.path("cssSelector").asText(null))
                        .violatedCode(node.path("violatedCode").asText(null))
                        .suggestion(node.path("suggestion").asText(null))
                        .build();

                parsed.add(WcagCheckResult.builder()
                        .wcagId(wcagId)
                        .wcagItemId(wcagItemId)
                        .title(title)
                        .judgeType(judgeType)
                        .violated(node.path("violated").asBoolean())
                        .filePath(node.path("filePath").asText(null))
                        .message(node.path("message").asText())
                        .locations(List.of(location))
                        .build());
            }

        } catch (Exception e) {
            return fallback(codeAiResults, aiResults);
        }

        return parsed;
    }

    private WcagCheckResult findOriginal(
            String wcagId,
            List<WcagCheckResult> codeAiResults,
            List<WcagCheckResult> aiResults
    ) {
        return Stream.concat(codeAiResults.stream(), aiResults.stream())
                .filter(r -> r.getWcagId().equals(wcagId))
                .findFirst()
                .orElse(null);
    }

    private List<WcagCheckResult> fallback(
            List<WcagCheckResult> codeAiResults,
            List<WcagCheckResult> aiResults
    ) {
        List<WcagCheckResult> all = new ArrayList<>();
        all.addAll(codeAiResults);
        all.addAll(aiResults);
        return all.stream()
                .map(r -> WcagCheckResult.builder()
                        .wcagId(r.getWcagId())
                        .wcagItemId(r.getWcagItemId())
                        .title(r.getTitle())
                        .judgeType(r.getJudgeType())
                        .violated(null)
                        .message("AI 응답 파싱 실패")
                        .filePath(r.getFilePath())
                        .locations(List.of())
                        .build())
                .toList();
    }

    private String truncate(String content) {
        if (content == null) return "";
        return content.length() > 3000
                ? content.substring(0, 3000) + "\n...[생략]"
                : content;
    }
}