package com.example.be_young04.domain.repository.controller;

import com.example.be_young04.domain.repository.dto.RepositoryFileResponse;
import com.example.be_young04.domain.repository.dto.RepositoryTreeResponse;
import com.example.be_young04.domain.repository.service.GithubRepositoryService;
import com.example.be_young04.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final GithubRepositoryService githubRepositoryService;

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<RepositoryTreeResponse>> getRepositoryTree(
            @RequestParam String repositoryUrl
    ) {
        RepositoryTreeResponse response = githubRepositoryService.getRepositoryTree(repositoryUrl);

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
            @RequestParam String repositoryUrl,
            @RequestParam String path
    ) {
        RepositoryFileResponse response = githubRepositoryService.getFileContent(repositoryUrl, path);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200REPO002",
                        "파일 내용 조회 성공",
                        response
                )
        );
    }
}