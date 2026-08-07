package com.example.be_young04.domain.git.controller;

import com.example.be_young04.domain.git.dto.request.GitCommitAndPushRequest;
import com.example.be_young04.domain.git.dto.request.GitCommitRequest;
import com.example.be_young04.domain.git.dto.request.GitPushRequest;
import com.example.be_young04.domain.git.dto.response.*;
import com.example.be_young04.domain.git.service.GitCommandService;
import com.example.be_young04.domain.git.service.GitQueryService;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/git")
public class GitController {

    private final GitQueryService gitQueryService;
    private final GitCommandService gitCommandService;

    @GetMapping("/status")
    public GitStatusResponse getStatus() {
        return gitQueryService.getStatus();
    }

    @GetMapping("/diff")
    public GitDiffResponse getDiff(
            @RequestParam String path
    ) {
        return gitQueryService.getDiff(path);
    }

    @GetMapping("/branches")
    public GitBranchResponse getBranches() {
        return gitQueryService.getBranches();
    }

    @PostMapping("/commit")
    public GitCommitResponse commit(
            @Valid @RequestBody GitCommitRequest request
    ) {
        return gitCommandService.commit(request);
    }

    @PostMapping("/push")
    public GitPushResponse push(
            @Valid @RequestBody GitPushRequest request
    ) {
        return gitCommandService.push(request);
    }

    @PostMapping("/commit-and-push")
    public GitCommitAndPushResponse commitAndPush(
            @Valid @RequestBody GitCommitAndPushRequest request
    ) {
        return gitCommandService.commitAndPush(request);
    }
}
