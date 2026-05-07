package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import com.example.be_young04.domain.analysis.checker.WcagCheckerRegistry;
import com.example.be_young04.domain.analysis.entity.AnalysisIssue;
import com.example.be_young04.domain.analysis.entity.AnalysisIssueLocation;
import com.example.be_young04.domain.analysis.entity.AnalysisRequest;
import com.example.be_young04.domain.analysis.entity.AnalysisResult;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueLocationRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisRequestRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisResultRepository;
import com.example.be_young04.domain.repository.dto.RepositoryFileResponse;
import com.example.be_young04.domain.repository.dto.RepositoryTreeResponse;
import com.example.be_young04.domain.repository.service.DBRepositoryService;
import com.example.be_young04.domain.repository.service.GithubRepositoryService;
import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import com.example.be_young04.domain.snapshot.service.SnapshotService;
import com.example.be_young04.domain.repository.entity.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WcagAnalysisService {

    private final GithubRepositoryService githubRepositoryService;
    private final DBRepositoryService dbRepositoryService;
    private final SnapshotService snapshotService;
    private final WcagCheckerRegistry wcagCheckerRegistry;
    private final AnalysisPromptBuilder analysisPromptBuilder;
    private final AiAnalysisClient aiAnalysisClient;
    private final AnalysisRequestRepository analysisRequestRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisIssueRepository analysisIssueRepository;
    private final AnalysisIssueLocationRepository analysisIssueLocationRepository;

    @Transactional
    public Long analyze(Long githubId, String repositoryUrl, String deploymentUrl, String branchName) {

        // Stage 1 — 분석 요청 저장 (PENDING)
        Repository repository = dbRepositoryService.getOrCreate(githubId, repositoryUrl);

        AnalysisRequest analysisRequest = analysisRequestRepository.save(
                AnalysisRequest.builder()
                        .repositoryId(repository.getRepositoryId())
                        .branchName(branchName)
                        .targetPath(repositoryUrl)
                        .status("PENDING")
                        .build()
        );

        try {
            analysisRequest.updateStatus("RUNNING");

            // Stage 1 — 파일 목록 수집
            RepositoryTreeResponse tree = githubRepositoryService
                    .getRepositoryTree(repositoryUrl);

            // Stage 2 — 스냅샷 캡처
            SnapshotResponse snapshot = snapshotService
                    .capture(deploymentUrl);

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
                String prompt = analysisPromptBuilder.build(
                        repositoryUrl,
                        snapshot.getImagePath(),
                        fileContents,
                        codeAiResults,
                        aiResults
                );

                String aiResponse = aiAnalysisClient.analyze(prompt);

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

            // Stage 5-1 — ANALYSIS_RESULTS 저장
            long failCount = finalResults.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getViolated()))
                    .count();
            long passCount = finalResults.stream()
                    .filter(r -> Boolean.FALSE.equals(r.getViolated()))
                    .count();
            long naCount = finalResults.stream()
                    .filter(r -> r.getViolated() == null)
                    .count();
            int overallScore = finalResults.isEmpty() ? 100
                    : (int) ((passCount * 100) / finalResults.size());

            AnalysisResult analysisResult = analysisResultRepository.save(
                    AnalysisResult.builder()
                            .analysisRequestId(analysisRequest.getAnalysisRequestId())
                            .summary("총 " + finalResults.size() + "개 항목 분석 완료")
                            .overallScore(overallScore)
                            .passCount((int) passCount)
                            .failCount((int) failCount)
                            .naCount((int) naCount)
                            .build()
            );

            // Stage 5-2~3 — ANALYSIS_ISSUES + LOCATIONS 저장
            for (WcagCheckResult result : finalResults) {
                if (!Boolean.TRUE.equals(result.getViolated())) continue;

                AnalysisIssue issue = analysisIssueRepository.save(
                        AnalysisIssue.builder()
                                .analysisResultId(analysisResult.getAnalysisResultId())
                                .issueType(result.getWcagId())
                                .levelType("A") // 추후 WCAG_ITEMS 연동 시 교체
                                .title(result.getTitle() != null ? result.getTitle() : result.getWcagId())
                                .description(result.getMessage() != null ? result.getMessage() : "")
                                .suggestion(result.getSuggestion() != null ? result.getSuggestion() : "")
                                .build()
                );

                if (result.getFilePath() != null) {
                    analysisIssueLocationRepository.save(
                            AnalysisIssueLocation.builder()
                                    .analysisIssueId(issue.getAnalysisIssueId())
                                    .targetFilePath(result.getFilePath())
                                    .originalCodeBlock(result.getViolatedCode() != null ? result.getViolatedCode() : "")
                                    .cssSelector(result.getCssSelector())
                                    .componentName(result.getComponentName())
                                    .build()
                    );
                }
            }

            analysisRequest.updateStatus("COMPLETE");
            return analysisResult.getAnalysisResultId();

        } catch (Exception e) {
            analysisRequest.updateStatus("FAIL");
            throw e;
        }
    }
}