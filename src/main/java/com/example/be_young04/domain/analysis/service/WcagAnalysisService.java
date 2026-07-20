package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.IssueLocation;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import com.example.be_young04.domain.analysis.checker.WcagCheckerRegistry;
import com.example.be_young04.domain.analysis.entity.AnalysisIssue;
import com.example.be_young04.domain.analysis.entity.AnalysisIssueLocation;
import com.example.be_young04.domain.analysis.entity.AnalysisWcagResult;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueLocationRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisIssueRepository;
import com.example.be_young04.domain.analysis.repository.AnalysisWcagResultRepository;
import com.example.be_young04.domain.analysis.service.AnalysisPromptBuilder.LocationJudgement;
import com.example.be_young04.domain.analysis.service.AnalysisPromptBuilder.PromptBuildResult;
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

    // 이미지 배치 한도 (벤더 중립 — 어떤 벤더든 안전하게 통과하는 보수적 기준)
    private static final int MAX_IMAGES_PER_BATCH = 10;
    private static final long MAX_BATCH_BYTES = 8L * 1024 * 1024;

    // location(텍스트) 배치 한도 — 프롬프트가 무한정 길어지는 것을 막기 위한 기준
    private static final int MAX_LOCATIONS_PER_BATCH = 30;

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

        RepositoryTreeResponse tree = githubRepositoryService.getRepositoryTree(repositoryUrl, branchName);

        Map<String, String> fileContents = new LinkedHashMap<>();
        Map<String, List<WcagCheckResult>> resultsByWcagId = new LinkedHashMap<>();

        for (RepositoryTreeResponse.TreeNode node : tree.getNodes()) {
            if (!"blob".equals(node.getType())) continue;

            List<WcagChecker> checkers = wcagCheckerRegistry.getCheckersFor(node.getPath());
            if (checkers.isEmpty()) continue;

            RepositoryFileResponse file = githubRepositoryService
                    .getFileContent(repositoryUrl, node.getPath(), branchName);

            fileContents.put(node.getPath(), file.getContent());

            for (WcagChecker checker : checkers) {
                WcagCheckResult result = checker.check(node.getPath(), file.getContent());
                if (result == null) continue;
                resultsByWcagId
                        .computeIfAbsent(result.getWcagId(), k -> new ArrayList<>())
                        .add(result);
            }
        }

        List<WcagCheckResult> codeAiResults = resultsByWcagId.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getJudgeType() == JudgeType.CODE_AI)
                .toList();

        List<WcagCheckResult> aiResults = resultsByWcagId.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getJudgeType() == JudgeType.AI)
                .toList();

        List<WcagCheckResult> codeOnlyResults = resultsByWcagId.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getJudgeType() == JudgeType.CODE)
                .toList();

        List<WcagCheckResult> finalResults;

        if (codeAiResults.isEmpty() && aiResults.isEmpty()) {
            finalResults = codeOnlyResults;
        } else {
            List<WcagCheckResult> aiTargets = new ArrayList<>();
            aiTargets.addAll(codeAiResults);
            aiTargets.addAll(aiResults);

            SnapshotMatchResult matchResult = groupBySnapshot(aiTargets, snapshots);

            List<LocationJudgement> allJudgements = runAiAnalysisInBatches(
                    repositoryUrl, snapshots, matchResult, fileContents
            );

            List<WcagCheckResult> aiReconstructedResults = reconstructFromJudgements(allJudgements);

            finalResults = new ArrayList<>();
            finalResults.addAll(codeOnlyResults);
            finalResults.addAll(aiReconstructedResults);
        }

        analysisWcagResultRepository.deleteByRepositoryId(repositoryId);
        saveResults(repositoryId, finalResults);
        repository.updateLastSyncedAt();

        return repositoryId;
    }

    /**
     * 이미지 배치와 fallback(텍스트 전용) 배치를 각각 독립적으로 구성하여 AI를 호출한다.
     * - 이미지 배치: 매칭된 스크린샷을 이미지 개수/용량 기준으로 나누고, 그 안의 location이 너무 많으면
     *   같은 이미지 세트를 공유하는 하위 배치로 추가 분할한다.
     * - fallback 배치: 이미지가 전혀 필요 없으므로 이미지 배치와 완전히 분리하여 location 개수 기준으로만 나눈다.
     *   (이미지 중복 첨부가 발생하지 않음)
     */
    private List<LocationJudgement> runAiAnalysisInBatches(
            String repositoryUrl,
            List<PageSnapshot> allSnapshots,
            SnapshotMatchResult matchResult,
            Map<String, String> fileContents
    ) {
        List<LocationJudgement> allJudgements = new ArrayList<>();

        // --- 1. 이미지 배치 처리 ---
        Set<String> usedSnapshotIds = matchResult.matched().stream()
                .flatMap(t -> t.snapshotIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<PageSnapshot> usedSnapshots = allSnapshots.stream()
                .filter(s -> usedSnapshotIds.contains(s.snapshotId()))
                .toList();

        List<List<PageSnapshot>> imageBatches = splitByImageLimit(usedSnapshots);

        Map<String, Integer> snapshotIdToBatchIndex = new HashMap<>();
        for (int i = 0; i < imageBatches.size(); i++) {
            for (PageSnapshot s : imageBatches.get(i)) {
                snapshotIdToBatchIndex.put(s.snapshotId(), i);
            }
        }

        List<List<MatchedTarget>> targetsPerImageBatch = new ArrayList<>();
        for (int i = 0; i < imageBatches.size(); i++) targetsPerImageBatch.add(new ArrayList<>());

        for (MatchedTarget target : matchResult.matched()) {
            Integer batchIndex = target.snapshotIds().stream()
                    .map(snapshotIdToBatchIndex::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);
            if (!imageBatches.isEmpty()) {
                targetsPerImageBatch.get(batchIndex).add(target);
            }
        }

        for (int i = 0; i < imageBatches.size(); i++) {
            List<PageSnapshot> batchSnapshots = imageBatches.get(i);
            List<MatchedTarget> batchTargets = targetsPerImageBatch.get(i);
            if (batchTargets.isEmpty()) continue;

            // location 수 기준으로 하위 분할 (같은 이미지 세트를 그대로 공유)
            for (List<MatchedTarget> subBatchTargets : splitTargetsByLocationLimit(batchTargets)) {
                allJudgements.addAll(callAiForBatch(
                        repositoryUrl, batchSnapshots, subBatchTargets, List.of(), fileContents
                ));
            }
        }

        // --- 2. fallback(텍스트 전용) 배치 처리 — 이미지와 완전히 독립 ---
        for (List<WcagCheckResult> fallbackBatch : splitResultsByLocationLimit(matchResult.fallback())) {
            allJudgements.addAll(callAiForBatch(
                    repositoryUrl, List.of(), List.of(), fallbackBatch, fileContents
            ));
        }

        return allJudgements;
    }

    private List<LocationJudgement> callAiForBatch(
            String repositoryUrl,
            List<PageSnapshot> batchSnapshots,
            List<MatchedTarget> batchTargets,
            List<WcagCheckResult> batchFallback,
            Map<String, String> fileContents
    ) {
        PromptBuildResult buildResult = analysisPromptBuilder.build(
                repositoryUrl, batchSnapshots, batchTargets, batchFallback, fileContents
        );

        List<byte[]> imageBytesList = batchSnapshots.stream()
                .map(this::readBytes)
                .toList();

        String aiResponse = aiAnalysisClient.analyze(buildResult.prompt(), imageBytesList);

        return analysisPromptBuilder.parseAiResponse(aiResponse, buildResult.locations());
    }

    /**
     * 이미지 리스트를 "이미지 장수 10장" 또는 "누적 용량 8MB" 중 먼저 도달하는 기준으로 배치 분할한다.
     */
    private List<List<PageSnapshot>> splitByImageLimit(List<PageSnapshot> snapshots) {
        List<List<PageSnapshot>> batches = new ArrayList<>();
        List<PageSnapshot> current = new ArrayList<>();
        long currentBytes = 0;

        for (PageSnapshot snapshot : snapshots) {
            long size = snapshot.image().getSize();

            boolean exceedsCount = current.size() >= MAX_IMAGES_PER_BATCH;
            boolean exceedsBytes = !current.isEmpty() && (currentBytes + size > MAX_BATCH_BYTES);

            if (!current.isEmpty() && (exceedsCount || exceedsBytes)) {
                batches.add(current);
                current = new ArrayList<>();
                currentBytes = 0;
            }

            current.add(snapshot);
            currentBytes += size;
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    /**
     * MatchedTarget 리스트를, 그 안에 담긴 location 총 개수가 MAX_LOCATIONS_PER_BATCH를 넘지 않도록 나눈다.
     * (target 하나가 location을 여러 개 가질 수 있으므로 target 단위가 아니라 location 개수로 계산한다)
     */
    private List<List<MatchedTarget>> splitTargetsByLocationLimit(List<MatchedTarget> targets) {
        List<List<MatchedTarget>> batches = new ArrayList<>();
        List<MatchedTarget> current = new ArrayList<>();
        int currentCount = 0;

        for (MatchedTarget target : targets) {
            int locCount = countLocations(target.result());

            if (!current.isEmpty() && currentCount + locCount > MAX_LOCATIONS_PER_BATCH) {
                batches.add(current);
                current = new ArrayList<>();
                currentCount = 0;
            }

            current.add(target);
            currentCount += locCount;
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    /**
     * fallback WcagCheckResult 리스트를 location 개수 기준으로 나눈다. (이미지 없음, 순수 텍스트 배치)
     */
    private List<List<WcagCheckResult>> splitResultsByLocationLimit(List<WcagCheckResult> results) {
        List<List<WcagCheckResult>> batches = new ArrayList<>();
        List<WcagCheckResult> current = new ArrayList<>();
        int currentCount = 0;

        for (WcagCheckResult result : results) {
            int locCount = countLocations(result);

            if (!current.isEmpty() && currentCount + locCount > MAX_LOCATIONS_PER_BATCH) {
                batches.add(current);
                current = new ArrayList<>();
                currentCount = 0;
            }

            current.add(result);
            currentCount += locCount;
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    private int countLocations(WcagCheckResult result) {
        List<IssueLocation> locations = result.getLocations();
        return (locations == null || locations.isEmpty()) ? 1 : locations.size();
    }

    private SnapshotMatchResult groupBySnapshot(List<WcagCheckResult> targets, List<PageSnapshot> snapshots) {
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

    private List<WcagCheckResult> reconstructFromJudgements(List<LocationJudgement> judgements) {
        Map<WcagCheckResult, List<LocationJudgement>> byOriginalResult = new IdentityHashMap<>();

        for (LocationJudgement judgement : judgements) {
            WcagCheckResult original = judgement.promptLocation().result();
            byOriginalResult.computeIfAbsent(original, k -> new ArrayList<>()).add(judgement);
        }

        List<WcagCheckResult> reconstructed = new ArrayList<>();

        for (Map.Entry<WcagCheckResult, List<LocationJudgement>> entry : byOriginalResult.entrySet()) {
            WcagCheckResult original = entry.getKey();

            List<IssueLocation> newLocations = entry.getValue().stream()
                    .map(j -> {
                        IssueLocation originalLocation = j.promptLocation().location();
                        return IssueLocation.builder()
                                .cssSelector(originalLocation != null ? originalLocation.getCssSelector() : null)
                                .violatedCode(originalLocation != null ? originalLocation.getViolatedCode() : null)
                                .suggestion(j.suggestion())
                                .violated(j.violated())
                                .build();
                    })
                    .toList();

            reconstructed.add(WcagCheckResult.builder()
                    .wcagId(original.getWcagId())
                    .wcagItemId(original.getWcagItemId())
                    .title(original.getTitle())
                    .judgeType(original.getJudgeType())
                    .violated(null)
                    .filePath(original.getFilePath())
                    .message(original.getMessage())
                    .locations(newLocations)
                    .build());
        }

        return reconstructed;
    }

    private void saveResults(Long repositoryId, List<WcagCheckResult> finalResults) {
        Map<Long, List<WcagCheckResult>> resultsByWcagItemId = finalResults.stream()
                .filter(r -> r.getWcagItemId() != null)
                .collect(Collectors.groupingBy(WcagCheckResult::getWcagItemId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<WcagCheckResult>> entry : resultsByWcagItemId.entrySet()) {
            Long wcagItemId = entry.getKey();
            List<WcagCheckResult> group = entry.getValue();

            WcagItem wcagItem = resolveWcagItem(wcagItemId);
            if (wcagItem == null) continue;

            List<Boolean> effectiveViolations = group.stream()
                    .flatMap(r -> effectiveViolations(r).stream())
                    .toList();

            boolean anyViolated = effectiveViolations.stream().anyMatch(Boolean.TRUE::equals);
            boolean anyPass = effectiveViolations.stream().anyMatch(Boolean.FALSE::equals);
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
                for (IssueLocation loc : result.getLocations()) {
                    Boolean effective = (loc.getViolated() != null) ? loc.getViolated() : result.getViolated();
                    if (!Boolean.TRUE.equals(effective)) continue;

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
    }

    private List<Boolean> effectiveViolations(WcagCheckResult result) {
        if (result.getLocations() == null || result.getLocations().isEmpty()) {
            return new ArrayList<>(Collections.singletonList(result.getViolated()));
        }
        return result.getLocations().stream()
                .map(loc -> loc.getViolated() != null ? loc.getViolated() : result.getViolated())
                .toList();
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