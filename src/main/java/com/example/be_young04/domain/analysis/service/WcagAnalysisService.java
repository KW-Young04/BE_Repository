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

    // 배치 한도 (벤더 중립 — Gemini/Bedrock 등 어떤 벤더든 안전하게 통과하는 보수적 기준)
    private static final int MAX_IMAGES_PER_BATCH = 10;
    private static final long MAX_BATCH_BYTES = 8L * 1024 * 1024;

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

        List<WcagCheckResult> codeOnlyResults = resultsByWcagId.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getJudgeType() == JudgeType.CODE)
                .toList();

        List<WcagCheckResult> finalResults;

        if (codeAiResults.isEmpty() && aiResults.isEmpty()) {
            finalResults = codeOnlyResults;
        } else {
            // Stage 4-2 — 스냅샷 매칭 (filePath 기준, 1:N)
            List<WcagCheckResult> aiTargets = new ArrayList<>();
            aiTargets.addAll(codeAiResults);
            aiTargets.addAll(aiResults);

            SnapshotMatchResult matchResult = groupBySnapshot(aiTargets, snapshots);

            // Stage 4-3~4 — 배치별 프롬프트 구성 + AI 호출 + 파싱
            List<LocationJudgement> allJudgements = runAiAnalysisInBatches(
                    repositoryUrl, snapshots, matchResult, fileContents
            );

            List<WcagCheckResult> aiReconstructedResults = reconstructFromJudgements(allJudgements);

            finalResults = new ArrayList<>();
            finalResults.addAll(codeOnlyResults);
            finalResults.addAll(aiReconstructedResults);
        }

        // Stage 5 — 재분석 정책: 기존 결과 삭제 후 재생성
        analysisWcagResultRepository.deleteByRepositoryId(repositoryId);
        saveResults(repositoryId, finalResults);

        return repositoryId;
    }

    /**
     * 매칭된 스크린샷만 골라 배치로 나누고, 배치마다 프롬프트 생성 + AI 호출 + 파싱하여
     * 전체 location 판단 결과를 하나로 모은다.
     */
    private List<LocationJudgement> runAiAnalysisInBatches(
            String repositoryUrl,
            List<PageSnapshot> allSnapshots,
            SnapshotMatchResult matchResult,
            Map<String, String> fileContents
    ) {
        // 실제로 매칭된 스크린샷만 사용 (매칭 안 된 스크린샷은 AI에게 보낼 필요 없음)
        Set<String> usedSnapshotIds = matchResult.matched().stream()
                .flatMap(t -> t.snapshotIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<PageSnapshot> usedSnapshots = allSnapshots.stream()
                .filter(s -> usedSnapshotIds.contains(s.snapshotId()))
                .toList();

        List<List<PageSnapshot>> batches = splitIntoBatches(usedSnapshots);
        if (batches.isEmpty()) {
            // 매칭된 이미지가 하나도 없는 경우 (전부 fallback) — 배치 1개(이미지 없음)로 처리
            batches = new ArrayList<>(List.of(List.of()));
        }

        // snapshotId -> 그 스냅샷이 속한 배치 인덱스
        Map<String, Integer> snapshotIdToBatchIndex = new HashMap<>();
        for (int i = 0; i < batches.size(); i++) {
            for (PageSnapshot s : batches.get(i)) {
                snapshotIdToBatchIndex.put(s.snapshotId(), i);
            }
        }

        // 배치별 담당 matched target 배정 (target의 snapshotId 중 첫 번째가 속한 배치로, first-match-wins)
        List<List<MatchedTarget>> targetsPerBatch = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) targetsPerBatch.add(new ArrayList<>());

        for (MatchedTarget target : matchResult.matched()) {
            Integer batchIndex = target.snapshotIds().stream()
                    .map(snapshotIdToBatchIndex::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);
            targetsPerBatch.get(batchIndex).add(target);
        }

        // fallback은 첫 번째 배치에 몰아서 처리
        List<WcagCheckResult> fallbackForFirstBatch = matchResult.fallback();

        List<LocationJudgement> allJudgements = new ArrayList<>();

        for (int i = 0; i < batches.size(); i++) {
            List<PageSnapshot> batchSnapshots = batches.get(i);
            List<MatchedTarget> batchTargets = targetsPerBatch.get(i);
            List<WcagCheckResult> batchFallback = (i == 0) ? fallbackForFirstBatch : List.of();

            if (batchTargets.isEmpty() && batchFallback.isEmpty()) continue;

            PromptBuildResult buildResult = analysisPromptBuilder.build(
                    repositoryUrl, batchSnapshots, batchTargets, batchFallback, fileContents
            );

            List<byte[]> imageBytesList = batchSnapshots.stream()
                    .map(this::readBytes)
                    .toList();

            String aiResponse = aiAnalysisClient.analyze(buildResult.prompt(), imageBytesList);

            allJudgements.addAll(analysisPromptBuilder.parseAiResponse(aiResponse, buildResult.locations()));
        }

        return allJudgements;
    }

    /**
     * 이미지 리스트를 "이미지 장수 10장" 또는 "누적 용량 8MB" 중 먼저 도달하는 기준으로 배치 분할한다.
     */
    private List<List<PageSnapshot>> splitIntoBatches(List<PageSnapshot> snapshots) {
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
     * CODE_AI/AI 판정이 필요한 결과들을, filePath 기준으로 "그 파일을 렌더링한 스크린샷들"과 매칭한다.
     * 하나의 filePath가 여러 스크린샷에 걸쳐 나올 수 있으므로 1:N 매칭이다.
     * 매칭되는 스크린샷이 하나도 없는 결과는 fallback 목록으로 분리한다.
     */
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

    /**
     * location 단위 AI 판단 결과를, 원본 result별로 다시 묶어 WcagCheckResult 형태로 복원한다.
     * result.violated는 더 이상 의미 없으므로 null로 두고, 실제 위반 여부는 각 IssueLocation.violated에 담는다.
     * 그룹핑은 원본 result 인스턴스의 참조(identity) 기준이다 — 내용이 같은 별개 인스턴스와 혼동되지 않도록
     * IdentityHashMap을 명시적으로 사용한다.
     */
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
                    .violated(null) // 이제 location 단위(IssueLocation.violated)로 대체됨
                    .filePath(original.getFilePath())
                    .message(original.getMessage())
                    .locations(newLocations)
                    .build());
        }

        return reconstructed;
    }

    /**
     * wcagItemId 기준 그룹핑 + 저장.
     * 위반 여부는 location 단위(IssueLocation.violated)를 우선 사용하고,
     * location에 값이 없으면(CODE 타입처럼 애초에 위치별 구분이 없는 경우) result.violated로 대체한다.
     */
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

    /**
     * result 하나가 가진 "실효 위반 여부" 목록.
     * - locations가 있으면 각 location마다: location.violated가 있으면 그 값, 없으면 result.violated로 대체
     * - locations가 비어 있으면(예: CODE 타입 PASS/NA로 위치 자체가 없는 경우) result.violated 하나를 그대로 사용
     */
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