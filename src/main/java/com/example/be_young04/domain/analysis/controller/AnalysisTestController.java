package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import com.example.be_young04.domain.analysis.checker.WcagCheckerRegistry;
import com.example.be_young04.domain.repository.dto.RepositoryFileResponse;
import com.example.be_young04.domain.repository.dto.RepositoryTreeResponse;
import com.example.be_young04.domain.repository.service.GithubRepositoryService;
import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import com.example.be_young04.domain.snapshot.service.SnapshotService;
import com.example.be_young04.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * [DUMMY] STAGE 1~3 흐름 테스트용 컨트롤러
 * 실제 구현 완료 후 삭제할 것
 */
@RestController
@RequestMapping("/api/test/analysis")
@RequiredArgsConstructor
public class AnalysisTestController {

    private final GithubRepositoryService githubRepositoryService;
    private final SnapshotService snapshotService;
    private final WcagCheckerRegistry wcagCheckerRegistry;

    @PostMapping
    public ResponseEntity<ApiResponse<AnalysisTestResponse>> test(
            @RequestBody AnalysisTestRequest request
    ) {
        // STAGE 1 — 파일 목록 수집
        RepositoryTreeResponse tree = githubRepositoryService
                .getRepositoryTree(request.repositoryUrl());

        // STAGE 2 — 스냅샷 캡처
        SnapshotResponse snapshot = snapshotService
                .capture(request.deploymentUrl());

        // STAGE 3 — 파일 순회 + wcagId 기준 결과 병합
        Map<String, List<String>> violatedFilesByWcagId = new LinkedHashMap<>();

        for (RepositoryTreeResponse.TreeNode node : tree.getNodes()) {
            if (!"blob".equals(node.getType())) continue;

            List<WcagChecker> checkers = wcagCheckerRegistry.getCheckersFor(node.getPath());
            if (checkers.isEmpty()) continue;

            RepositoryFileResponse file = githubRepositoryService
                    .getFileContent(request.repositoryUrl(), node.getPath());

            for (WcagChecker checker : checkers) {
                WcagCheckResult result = checker.check(node.getPath(), file.getContent());
                if (Boolean.TRUE.equals(result.getViolated())) {
                    violatedFilesByWcagId
                            .computeIfAbsent(result.getWcagId(), k -> new ArrayList<>())
                            .add(node.getPath());
                }
            }
        }

        // 병합된 결과 생성
        List<MergedResult> mergedResults = violatedFilesByWcagId.entrySet().stream()
                .map(entry -> new MergedResult(entry.getKey(), entry.getValue()))
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200TEST001",
                        "STAGE 1~3 테스트 완료",
                        new AnalysisTestResponse(snapshot.getImagePath(), mergedResults)
                )
        );
    }

    // [DUMMY] 요청 DTO
    record AnalysisTestRequest(String repositoryUrl, String deploymentUrl) {}

    // [DUMMY] 응답 DTO
    record AnalysisTestResponse(String snapshotPath, List<MergedResult> results) {}

    // [DUMMY] 병합 결과
    record MergedResult(String wcagId, List<String> violatedFiles) {}
}