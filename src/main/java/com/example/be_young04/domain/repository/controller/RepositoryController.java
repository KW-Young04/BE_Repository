package com.example.be_young04.domain.repository.controller;

import com.example.be_young04.domain.repository.dto.RepositoryBranchFileRequest;
import com.example.be_young04.domain.repository.dto.RepositoryBranchRequest;
import com.example.be_young04.domain.repository.dto.RepositoryFileResponse;
import com.example.be_young04.domain.repository.dto.RepositoryTreeResponse;
import com.example.be_young04.domain.repository.service.GithubRepositoryService;
import com.example.be_young04.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final GithubRepositoryService githubRepositoryService;

    @PostMapping("/branch")
    public ResponseEntity<ApiResponse<RepositoryTreeResponse>> selectRepositoryBranch(
            @AuthenticationPrincipal Long githubId,
            @RequestBody RepositoryBranchRequest request
    ) {
        RepositoryTreeResponse response = githubRepositoryService.getRepositoryTree(
                githubId,
                request.getRepositoryUrl(),
                request.getBranchName()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200REPO003",
                        "Repository branch tree loaded successfully",
                        response
                )
        );
    }

    @PostMapping("/branch/file")
    public ResponseEntity<ApiResponse<RepositoryFileResponse>> getBranchFileContent(
            @AuthenticationPrincipal Long githubId,
            @RequestBody RepositoryBranchFileRequest request
    ) {
        RepositoryFileResponse response = githubRepositoryService.getFileContent(
                githubId,
                request.getRepositoryUrl(),
                request.getPath(),
                request.getBranchName()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200REPO004",
                        "Repository branch file loaded successfully",
                        response
                )
        );
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<RepositoryTreeResponse>> getRepositoryTree(
            @AuthenticationPrincipal Long githubId,
            @RequestParam String repositoryUrl
    ) {
        RepositoryTreeResponse response = githubRepositoryService.getRepositoryTree(githubId, repositoryUrl);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200REPO001",
                        "저장소 트리 구조 조회 성공",
                        response
                )
        );
    }

    @GetMapping("/file")
    public ResponseEntity<ApiResponse<RepositoryFileResponse>> getFileContent(
            @AuthenticationPrincipal Long githubId,
            @RequestParam String repositoryUrl,
            @RequestParam String path,
            @RequestParam(required = false) String branchName
    ) {
        RepositoryFileResponse response = githubRepositoryService.getFileContent(
                githubId,
                repositoryUrl,
                path,
                branchName
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200REPO002",
                        "파일 내용 조회 성공",
                        response
                )
        );
    }
}
