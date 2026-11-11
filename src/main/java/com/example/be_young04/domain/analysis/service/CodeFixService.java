package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.dto.CodeFixDto.*;
import com.example.be_young04.domain.analysis.entity.AnalysisIssue;
import com.example.be_young04.domain.analysis.entity.AnalysisIssueLocation;
import com.example.be_young04.domain.analysis.entity.AnalysisWcagResult;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueLocationRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisWcagResultRepository;
import com.example.be_young04.domain.git.dto.request.GitFileWriteRequest;
import com.example.be_young04.domain.git.service.GitWorkingTreeService;
import com.example.be_young04.domain.git.service.GitWorkspaceManager;
import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.realtime_analysis.service.RealtimeAnalysisService;
import com.example.be_young04.domain.repository.repository.RepositoryRepository;
import com.example.be_young04.domain.wcag.entity.WcagItem;
import com.example.be_young04.domain.wcag.repository.WcagItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 코드 수정: 위반 위치별 {before, after} 패치를 생성하고, 사용자가 승인한 것만 작업 파일에 적용한다.
 * - 생성: 파일 단위 1회 호출 (MAX_LOCATIONS_PER_CALL 초과 시에만 분할)
 * - 검증: before가 현재 파일에 정확히 1회 등장 + 다른 수정안과 범위 비겹침
 * - 부분 실패 허용: 검증 실패 위치만 FAILED, 나머지는 정상 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeFixService {

    private static final int MAX_LOCATIONS_PER_CALL = 30;

    private final AiAnalysisClient aiAnalysisClient;
    private final GitWorkspaceManager gitWorkspaceManager;
    private final GitRepositoryValidator gitRepositoryValidator;
    private final GitWorkingTreeService gitWorkingTreeService;
    private final RealtimeAnalysisService realtimeAnalysisService;
    private final RepositoryRepository repositoryRepository;
    private final WcagItemRepository wcagItemRepository;
    private final AnalysisWcagResultRepository analysisWcagResultRepository;
    private final AnalysisIssueRepository analysisIssueRepository;
    private final AnalysisIssueLocationRepository analysisIssueLocationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record FixTarget(String promptId, AnalysisIssueLocation location, WcagItem wcagItem) {
    }

    private record RawFix(String before, String after) {
    }

    // ===================== 생성 =====================

    // AI 호출이 길어질 수 있어 트랜잭션을 걸지 않고, 마지막에 saveAll로 저장한다.
    public GenerateResponse generate(Long githubId, Long repositoryId, GenerateRequest request) {
        verifyOwner(githubId, repositoryId);

        List<FixTarget> targets = loadTargets(repositoryId, request.filePath()).stream()
                .filter(t -> "OPEN".equals(t.location().getStatus()))
                .toList();
        if (targets.isEmpty()) {
            return new GenerateResponse(request.filePath(), List.of());
        }

        String content = gitWorkspaceManager.withWorkspace(
                githubId, request.repositoryUrl(), request.branchName(),
                workspace -> readFile(workspace.repositoryPath(), request.filePath())
        );

        Map<String, RawFix> rawFixes = new HashMap<>();
        for (int i = 0; i < targets.size(); i += MAX_LOCATIONS_PER_CALL) {
            List<FixTarget> batch = targets.subList(i, Math.min(i + MAX_LOCATIONS_PER_CALL, targets.size()));
            rawFixes.putAll(requestFixes(batch, request.filePath(), content));
        }

        return new GenerateResponse(request.filePath(), validateAndSave(targets, rawFixes, content));
    }

    private Map<String, RawFix> requestFixes(List<FixTarget> batch, String filePath, String content) {
        String response = aiAnalysisClient.analyze(buildPrompt(batch, filePath, content), List.of());
        Set<String> validIds = batch.stream().map(FixTarget::promptId).collect(Collectors.toSet());
        Map<String, RawFix> result = new HashMap<>();

        try {
            String json = response.replaceAll("```json", "").replaceAll("```", "").trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start < 0 || end <= start) {
                log.warn("[CodeFix] JSON 배열 없음: {}", response);
                return result;
            }

            for (JsonNode node : objectMapper.readTree(json.substring(start, end + 1))) {
                String id = node.path("locationId").asText(null);
                if (id == null || !validIds.contains(id)) continue;   // 환각 locationId 무시
                result.putIfAbsent(id, new RawFix(
                        node.hasNonNull("before") ? node.get("before").asText() : null,
                        node.hasNonNull("after") ? node.get("after").asText() : null
                ));
            }
        } catch (Exception e) {
            log.warn("[CodeFix] 응답 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    private List<FixItem> validateAndSave(List<FixTarget> targets, Map<String, RawFix> rawFixes, String content) {
        List<FixItem> items = new ArrayList<>();
        List<AnalysisIssueLocation> updated = new ArrayList<>();
        List<int[]> takenRanges = new ArrayList<>();

        for (FixTarget target : targets) {
            Long locationId = target.location().getAnalysisIssueLocationId();
            String sc = target.wcagItem().getSc();
            RawFix fix = rawFixes.get(target.promptId());

            String failReason = null;
            if (fix == null || fix.before() == null || fix.before().isEmpty() || fix.after() == null) {
                failReason = "AI가 이 위치의 수정안을 생성하지 못했습니다.";
            } else if (fix.before().equals(fix.after())) {
                failReason = "변경 내용이 없습니다.";
            } else {
                int count = countOccurrences(content, fix.before());
                if (count == 0) {
                    failReason = "수정 대상 코드를 현재 파일에서 찾을 수 없습니다.";
                } else if (count > 1) {
                    failReason = "수정 대상 코드가 파일에 여러 번 있어 위치를 특정할 수 없습니다.";
                } else {
                    int start = content.indexOf(fix.before());
                    int end = start + fix.before().length();
                    if (overlaps(takenRanges, start, end)) {
                        failReason = "다른 수정안과 수정 범위가 겹칩니다.";
                    } else {
                        takenRanges.add(new int[]{start, end});
                    }
                }
            }

            if (failReason != null) {
                items.add(FixItem.failed(locationId, sc, failReason));
                continue;
            }

            target.location().updateCodeFix(toDetailJson(fix));
            updated.add(target.location());
            items.add(FixItem.ready(locationId, sc, fix.before(), fix.after()));
        }

        analysisIssueLocationRepository.saveAll(updated);
        return items;
    }

    // 응답 형식 지시문은 최상단에 둔다 (프롬프트가 길어져도 지시문이 밀리지 않게).
    private String buildPrompt(List<FixTarget> batch, String filePath, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                너는 WCAG 2.2 웹 접근성 코드 수정 전문가다.
                아래 [수정 대상 위반 목록]의 각 위치를 고치는 코드 패치를 만들어라.

                [응답 형식] 아래 JSON 배열만 출력하고, 다른 텍스트는 절대 쓰지 마라.
                [
                  {"locationId": "L1", "before": "원본 코드 구간", "after": "수정된 코드 구간"}
                ]

                [규칙]
                1. before는 [파일 전체]에 공백·줄바꿈·따옴표까지 글자 하나 다르지 않게 존재하는 연속 구간이어야 한다.
                2. before는 파일 안에서 정확히 한 번만 등장해야 한다. 같은 코드가 여러 번 나오면 앞뒤 줄을 포함해 유일하게 만든다.
                3. before는 위반을 고치는 데 필요한 최소 범위로 잡는다. 파일 전체를 넣지 않는다.
                4. after는 before를 그대로 대체할 코드다. 위반 해결에 필요한 부분만 바꾸고 들여쓰기, 코드 스타일, 기존 동작은 유지한다.
                5. 서로 다른 locationId의 before 구간이 겹치면 안 된다.
                6. 코드만으로 해결할 수 없는 항목(자막 파일 제작 등)은 배열에서 생략한다.
                7. alt 텍스트처럼 내용을 지어내야 하면 주변 맥락으로 추론하되, 근거가 부족하면 "[설명 입력 필요]"를 넣는다.

                """);

        sb.append("[파일 경로] ").append(filePath).append("\n\n");
        sb.append("[수정 대상 위반 목록]\n");
        for (FixTarget t : batch) {
            sb.append("- ").append(t.promptId())
                    .append(" | SC ").append(t.wcagItem().getSc()).append(' ').append(t.wcagItem().getTitle()).append('\n')
                    .append("  기존 안내: ").append(Objects.toString(t.location().getSuggestion(), "")).append('\n')
                    .append("  위반 코드:\n```\n").append(t.location().getOriginalCodeBlock()).append("\n```\n");
        }

        sb.append("\n[파일 전체]\n```\n").append(content).append("\n```\n");
        return sb.toString();
    }

    // ===================== 적용 =====================

    public ApplyResponse apply(Long githubId, Long repositoryId, ApplyRequest request) {
        verifyOwner(githubId, repositoryId);

        // 이 저장소 + 이 파일에 속한 위치만 허용 (다른 저장소 locationId 차단)
        Map<Long, FixTarget> targetById = loadTargets(repositoryId, request.filePath()).stream()
                .collect(Collectors.toMap(t -> t.location().getAnalysisIssueLocationId(), t -> t));

        return gitWorkspaceManager.withWorkspace(
                githubId, request.repositoryUrl(), request.branchName(),
                workspace -> {
                    Path repoPath = workspace.repositoryPath();
                    String content = readFile(repoPath, request.filePath());

                    List<Long> applied = new ArrayList<>();
                    List<FixItem> failed = new ArrayList<>();
                    List<AnalysisIssueLocation> modified = new ArrayList<>();

                    for (Long locationId : request.locationIds()) {
                        FixTarget target = targetById.get(locationId);
                        if (target == null) {
                            failed.add(FixItem.failed(locationId, null, "이 파일의 위반 위치가 아닙니다."));
                            continue;
                        }

                        String sc = target.wcagItem().getSc();
                        RawFix fix = parseDetail(target.location());
                        if (fix == null) {
                            failed.add(FixItem.failed(locationId, sc, "생성된 수정안이 없습니다."));
                            continue;
                        }

                        int count = countOccurrences(content, fix.before());
                        if (count != 1) {
                            failed.add(FixItem.failed(locationId, sc, count == 0
                                    ? "파일이 변경되어 수정 대상 코드를 찾을 수 없습니다. 수정안을 다시 생성하세요."
                                    : "수정 대상 코드가 여러 번 있어 위치를 특정할 수 없습니다."));
                            continue;
                        }

                        content = content.replace(fix.before(), fix.after());
                        target.location().markModified();
                        modified.add(target.location());
                        applied.add(locationId);
                    }

                    if (!applied.isEmpty()) {
                        gitWorkingTreeService.writeFile(repoPath, new GitFileWriteRequest(
                                request.repositoryUrl(), request.branchName(), request.filePath(), content));
                        analysisIssueLocationRepository.saveAll(modified);
                    }

                    // 적용 결과를 정적 룰로 즉시 재검사 (AI 호출 없음)
                    RealtimeAnalysisResponse remaining = realtimeAnalysisService.analyzeCode(content, request.filePath());
                    return new ApplyResponse(request.filePath(), applied, failed, remaining);
                }
        );
    }

    // ===================== 공통 =====================

    private void verifyOwner(Long githubId, Long repositoryId) {
        if (githubId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        repositoryRepository.findByRepositoryIdAndGithubId(repositoryId, githubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장소를 찾을 수 없습니다."));
    }

    // 저장소의 FAIL 결과 → 이슈 → 위치를 따라가 해당 파일의 위치만 수집한다.
    private List<FixTarget> loadTargets(Long repositoryId, String filePath) {
        Map<Long, Long> wcagItemIdByResultId = analysisWcagResultRepository.findByRepositoryId(repositoryId).stream()
                .filter(r -> "FAIL".equals(r.getStatus()))
                .collect(Collectors.toMap(AnalysisWcagResult::getAnalysisWcagResultId,
                        AnalysisWcagResult::getWcagItemId, (a, b) -> a));
        if (wcagItemIdByResultId.isEmpty()) return List.of();

        Map<Long, Long> wcagItemIdByIssueId = analysisIssueRepository
                .findByAnalysisWcagResultIdIn(new ArrayList<>(wcagItemIdByResultId.keySet())).stream()
                .collect(Collectors.toMap(AnalysisIssue::getAnalysisIssueId,
                        issue -> wcagItemIdByResultId.get(issue.getAnalysisWcagResultId()), (a, b) -> a));
        if (wcagItemIdByIssueId.isEmpty()) return List.of();

        Map<Long, WcagItem> wcagItemById = wcagItemRepository
                .findAllById(new HashSet<>(wcagItemIdByIssueId.values())).stream()
                .collect(Collectors.toMap(WcagItem::getWcagItemId, item -> item));

        List<FixTarget> targets = new ArrayList<>();
        int seq = 1;
        for (AnalysisIssueLocation location : analysisIssueLocationRepository
                .findByAnalysisIssueIdIn(new ArrayList<>(wcagItemIdByIssueId.keySet()))) {
            if (!filePath.equals(location.getTargetFilePath())) continue;
            WcagItem item = wcagItemById.get(wcagItemIdByIssueId.get(location.getAnalysisIssueId()));
            if (item == null) continue;
            targets.add(new FixTarget("L" + seq++, location, item));
        }
        return targets;
    }

    private String readFile(Path repositoryPath, String filePath) {
        gitRepositoryValidator.validateRepository(repositoryPath);
        String relativePath = gitRepositoryValidator.validateFilePath(repositoryPath, filePath);
        try {
            return Files.readString(repositoryPath.resolve(relativePath).normalize(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 읽을 수 없습니다: " + filePath);
        }
    }

    private RawFix parseDetail(AnalysisIssueLocation location) {
        if (!"CODE_FIX".equals(location.getSuggestionType()) || location.getSuggestionDetail() == null) return null;
        try {
            JsonNode node = objectMapper.readTree(location.getSuggestionDetail());
            String before = node.path("before").asText("");
            if (before.isEmpty() || !node.has("after")) return null;
            return new RawFix(before, node.get("after").asText());
        } catch (Exception e) {
            return null;
        }
    }

    private String toDetailJson(RawFix fix) {
        return objectMapper.createObjectNode()
                .put("before", fix.before())
                .put("after", fix.after())
                .toString();
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }

    private boolean overlaps(List<int[]> ranges, int start, int end) {
        return ranges.stream().anyMatch(r -> start < r[1] && r[0] < end);
    }
}