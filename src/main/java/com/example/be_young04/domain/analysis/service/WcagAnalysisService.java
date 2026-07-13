package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import com.example.be_young04.domain.analysis.checker.WcagCheckerRegistry;
import com.example.be_young04.domain.analysis.entity.AnalysisIssue;
import com.example.be_young04.domain.analysis.entity.AnalysisIssueLocation;
import com.example.be_young04.domain.analysis.entity.AnalysisWcagResult;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueLocationRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisWcagResultRepository;
import com.example.be_young04.domain.repository.dto.RepositoryFileResponse;
import com.example.be_young04.domain.repository.dto.RepositoryTreeResponse;
import com.example.be_young04.domain.repository.entity.Repository;
import com.example.be_young04.domain.repository.service.DBRepositoryService;
import com.example.be_young04.domain.repository.service.GithubRepositoryService;
import com.example.be_young04.domain.snapshot.dto.PageSnapshot;
import com.example.be_young04.domain.wcag.entity.WcagItem;
import com.example.be_young04.domain.wcag.repository.WcagItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WcagAnalysisService {

    private final GithubRepositoryService githubRepositoryService;
    private final DBRepositoryService dbRepositoryService;
    private final WcagCheckerRegistry wcagCheckerRegistry;
    private final AnalysisPromptBuilder analysisPromptBuilder;
    private final AiAnalysisClient aiAnalysisClient;
    private final WcagItemRepository wcagItemRepository;
    private final AnalysisWcagResultRepository analysisWcagResultRepository;
    private final AnalysisIssueRepository analysisIssueRepository;
    private final AnalysisIssueLocationRepository analysisIssueLocationRepository;

    @Transactional
    public Long analyze(Long githubId, String repositoryUrl, String branchName, List<PageSnapshot> snapshots) {

        Repository repository = dbRepositoryService.getOrCreate(githubId, repositoryUrl);
        Long repositoryId = repository.getRepositoryId();

        // Stage 1 — 파일 목록 수집
        RepositoryTreeResponse tree = githubRepositoryService.getRepositoryTree(repositoryUrl);

        // Stage 3 — 파일 순회 + 체커 실행 + wcagId 기준 병합
        Map<String, String> fileContents = new LinkedHashMap<>();
        Map<String, List<WcagCheckResult>> resultsByWcagId = new LinkedHashMap<>();

        for (RepositoryTreeResponse.TreeNode node : tree.getNodes()) {
            if (!"blob".equals(node.getType())) continue;

            List<WcagChecker> checkers = wcagCheckerRegistry.getCheckersFor(node.getPath());
            if (checkers.isEmpty()) continue;

            RepositoryFileResponse file = githubRepositoryService
                    .getFileContent(repositoryUrl, node.getPath());

            fileContents.put(node.getPath(), file.getContent());

            for (WcagChecker checker : checkers) {
                WcagCheckResult result = checker.check(node.getPath(), file.getContent());
                if (result == null) continue;
                resultsByWcagId
                        .computeIfAbsent(result.getWcagId(), k -> new ArrayList<>())
                        .add(result);
            }
        }

        // Stage 4-1 — CODE_AI, AI 항목 필터링
        List<WcagCheckResult> codeAiResults = resultsByWcagId.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getJudgeType() == JudgeType.CODE_AI)
                .toList();

        List<WcagCheckResult> aiResults = resultsByWcagId.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getJudgeType() == JudgeType.AI)
                .toList();

        List<WcagCheckResult> finalResults;

        if (codeAiResults.isEmpty() && aiResults.isEmpty()) {
            finalResults = resultsByWcagId.values().stream()
                    .flatMap(List::stream)
                    .toList();
        } else {
            // Stage 4-2 — 스냅샷 매칭 (filePath 기준, 1:N)
            List<WcagCheckResult> aiTargets = new ArrayList<>();
            aiTargets.addAll(codeAiResults);
            aiTargets.addAll(aiResults);

            SnapshotMatchResult matchResult = groupBySnapshot(aiTargets, snapshots);

            // TODO(3단계): AnalysisPromptBuilder를 이미지 여러 장 + locationId 배열 응답 구조로 재설계.
            // 지금은 컴파일 통과 + 기존 흐름 유지를 위해 매칭 결과 중 대표 스냅샷 1장만 사용하는 임시 스텁.
            boolean hasSnapshot = !snapshots.isEmpty();
            byte[] snapshotImageBytes = hasSnapshot ? readBytes(snapshots.get(0)) : null;

            String prompt = analysisPromptBuilder.build(
                    repositoryUrl,
                    hasSnapshot,
                    fileContents,
                    codeAiResults,
                    aiResults
            );

            String aiResponse = aiAnalysisClient.analyze(prompt, snapshotImageBytes);

            List<WcagCheckResult> aiParsedResults = analysisPromptBuilder.parseAiResponse(
                    aiResponse,
                    codeAiResults,
                    aiResults
            );

            List<WcagCheckResult> codeOnlyResults = resultsByWcagId.values().stream()
                    .flatMap(List::stream)
                    .filter(r -> r.getJudgeType() == JudgeType.CODE)
                    .toList();

            finalResults = new ArrayList<>();
            finalResults.addAll(codeOnlyResults);
            finalResults.addAll(aiParsedResults);

            // 참고용 로그 — 3단계에서 matchResult를 실제 프롬프트 구성에 사용하게 됨
            logMatchSummary(matchResult);
        }

        // Stage 5 — 재분석 정책: 기존 결과 삭제 후 재생성
        analysisWcagResultRepository.deleteByRepositoryId(repositoryId);

        // wcagItemId 기준으로 그룹핑 (같은 항목이 여러 파일에서 나와도 하나로 병합)
        Map<Long, List<WcagCheckResult>> resultsByWcagItemId = finalResults.stream()
                .filter(r -> r.getWcagItemId() != null)
                .collect(Collectors.groupingBy(WcagCheckResult::getWcagItemId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<WcagCheckResult>> entry : resultsByWcagItemId.entrySet()) {
            Long wcagItemId = entry.getKey();
            List<WcagCheckResult> group = entry.getValue();

            WcagItem wcagItem = resolveWcagItem(wcagItemId);
            if (wcagItem == null) continue;

            boolean anyViolated = group.stream().anyMatch(r -> Boolean.TRUE.equals(r.getViolated()));
            boolean anyPass = group.stream().anyMatch(r -> Boolean.FALSE.equals(r.getViolated()));
            String status = anyViolated ? "FAIL" : (anyPass ? "PASS" : "NA");

            AnalysisWcagResult wcagResult = analysisWcagResultRepository.save(
                    AnalysisWcagResult.builder()
                            .repositoryId(repositoryId)
                            .wcagItemId(wcagItem.getWcagItemId())
                            .status(status)
                            .build()
            );

            if (!"FAIL".equals(status)) continue;

            AnalysisIssue issue = analysisIssueRepository.save(
                    AnalysisIssue.builder()
                            .analysisWcagResultId(wcagResult.getAnalysisWcagResultId())
                            .build()
            );

            for (WcagCheckResult result : group) {
                if (!Boolean.TRUE.equals(result.getViolated())) continue;

                for (WcagCheckResult.IssueLocation loc : result.getLocations()) {
                    analysisIssueLocationRepository.save(
                            AnalysisIssueLocation.builder()
                                    .analysisIssueId(issue.getAnalysisIssueId())
                                    .targetFilePath(result.getFilePath())
                                    .targetSelector(loc.getCssSelector())
                                    .originalCodeBlock(loc.getViolatedCode() != null ? loc.getViolatedCode() : "")
                                    .suggestion(loc.getSuggestion() != null ? loc.getSuggestion() : "")
                                    .build()
                    );
                }
            }
        }

        return repositoryId;
    }

    /**
     * CODE_AI/AI 판정이 필요한 결과들을, filePath 기준으로 "그 파일을 렌더링한 스크린샷들"과 매칭한다.
     * 하나의 filePath가 여러 스크린샷에 걸쳐 나올 수 있으므로 1:N 매칭이다.
     * 매칭되는 스크린샷이 하나도 없는 결과는 fallback 목록으로 분리한다.
     */
    private SnapshotMatchResult groupBySnapshot(List<WcagCheckResult> targets, List<PageSnapshot> snapshots) {
        // filePath -> 그 파일을 렌더링한 모든 스크린샷의 snapshotId 목록
        Map<String, List<String>> filePathToSnapshotIds = new LinkedHashMap<>();
        for (PageSnapshot snapshot : snapshots) {
            for (String path : snapshot.renderedFilePaths()) {
                filePathToSnapshotIds
                        .computeIfAbsent(path, k -> new ArrayList<>())
                        .add(snapshot.snapshotId());
            }
        }

        List<MatchedTarget> matched = new ArrayList<>();
        List<WcagCheckResult> fallback = new ArrayList<>();

        for (WcagCheckResult result : targets) {
            List<String> snapshotIds = (result.getFilePath() != null)
                    ? filePathToSnapshotIds.get(result.getFilePath())
                    : null;

            if (snapshotIds == null || snapshotIds.isEmpty()) {
                fallback.add(result);
            } else {
                matched.add(new MatchedTarget(result, snapshotIds));
            }
        }

        return new SnapshotMatchResult(matched, fallback);
    }

    private void logMatchSummary(SnapshotMatchResult matchResult) {
        // TODO(3단계): 실제 프롬프트 구성 시 matchResult.matched()/fallback()을 그대로 사용 예정.
        // 지금은 매칭 로직 검증용 임시 로그.
        System.out.printf(
                "[WCAG 매칭] 이미지 매칭 %d건, fallback(이미지 없음) %d건%n",
                matchResult.matched().size(),
                matchResult.fallback().size()
        );
    }

    private byte[] readBytes(PageSnapshot snapshot) {
        try {
            return snapshot.image().getBytes();
        } catch (Exception e) {
            throw new IllegalStateException("스냅샷 이미지를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private WcagItem resolveWcagItem(Long wcagItemId) {
        if (wcagItemId == null) return null;
        return wcagItemRepository.findById(wcagItemId).orElse(null);
    }

    /**
     * 판정 대상 결과 하나와, 그 결과의 filePath를 렌더링한 스냅샷 id 목록(1:N)의 짝.
     */
    private record MatchedTarget(WcagCheckResult result, List<String> snapshotIds) {
    }

    /**
     * 스냅샷 매칭 결과.
     * matched: 이미지와 매칭된 (결과, 스냅샷id목록) 쌍의 리스트
     * fallback: 매칭되는 스냅샷이 없어 이미지 없이 텍스트만으로 판단해야 하는 결과 목록
     */
    private record SnapshotMatchResult(
            List<MatchedTarget> matched,
            List<WcagCheckResult> fallback
    ) {
    }
}