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

        // Stage 2 — 스냅샷 수신
        // TODO(2단계): filePath ↔ snapshot 매칭 로직으로 교체 예정.
        // 지금은 컴파일 통과를 위해 첫 번째 스냅샷 이미지만 임시로 사용.
        boolean hasSnapshot = !snapshots.isEmpty();
        byte[] snapshotImageBytes = hasSnapshot ? readBytes(snapshots.get(0)) : null;

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
            // Stage 4-2~4 — 프롬프트 구성 + Gemini 호출 + 파싱
            // TODO(3단계): 이미지 단위 그룹핑 + locationId 배열 응답 구조로 재설계 예정
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
}