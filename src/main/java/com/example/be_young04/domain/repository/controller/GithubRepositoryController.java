package com.example.be_young04.domain.repository.controller;

import com.example.be_young04.domain.repository.dto.GithubRepositoryResponse;
import com.example.be_young04.domain.repository.service.GithubRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/github/repositories")
public class GithubRepositoryController {

    private final GithubRepositoryService githubRepositoryService;

    @GetMapping("/recent")
    public List<GithubRepositoryResponse> getRecentRepositories(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        String accessToken = extractBearerToken(authorizationHeader);

        return githubRepositoryService.getRecentRepositories(accessToken);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더는 Bearer 토큰 형식이어야 합니다.");
        }

        return authorizationHeader.substring(7);
    }
}