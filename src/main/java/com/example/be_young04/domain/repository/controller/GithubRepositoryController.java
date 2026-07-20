package com.example.be_young04.domain.repository.controller;

import com.example.be_young04.domain.repository.dto.RecentRepositoryResponse;
import com.example.be_young04.domain.repository.service.DBRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/github/repositories")
public class GithubRepositoryController {

    private final DBRepositoryService dbRepositoryService;

    @GetMapping("/recent")
    public List<RecentRepositoryResponse> getRecentRepositories(
            @AuthenticationPrincipal Long githubId
    ) {
        return dbRepositoryService.getRecentRepositories(githubId);
    }
}