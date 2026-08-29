package com.example.be_young04.domain.git.controller;

import com.example.be_young04.domain.git.dto.request.GitCommitAndPushRequest;
import com.example.be_young04.domain.git.dto.request.GitCommitRequest;
import com.example.be_young04.domain.git.dto.request.GitFileWriteRequest;
import com.example.be_young04.domain.git.dto.request.GitPushRequest;
import com.example.be_young04.domain.git.dto.response.*;
import com.example.be_young04.domain.git.service.GitCommandService;
import com.example.be_young04.domain.git.service.GitQueryService;
import com.example.be_young04.domain.git.service.GitWorkingTreeService;
import com.example.be_young04.domain.git.service.GitWorkspaceManager;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/git")
public class GitController {

    private final GitQueryService gitQueryService;
    private final GitCommandService gitCommandService;
    private final GitWorkingTreeService gitWorkingTreeService;
    private final GitWorkspaceManager gitWorkspaceManager;

    @GetMapping("/status")
    public GitStatusResponse getStatus(
            @AuthenticationPrincipal Long githubId,
            @RequestParam String repositoryUrl,
            @RequestParam String branchName
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                repositoryUrl,
                branchName,
                workspace -> gitQueryService.getStatus(workspace.repositoryPath())
        );
    }

    @GetMapping("/diff")
    public GitDiffResponse getDiff(
            @AuthenticationPrincipal Long githubId,
            @RequestParam String repositoryUrl,
            @RequestParam String branchName,
            @RequestParam String path
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                repositoryUrl,
                branchName,
                workspace -> gitQueryService.getDiff(workspace.repositoryPath(), path)
        );
    }

    @GetMapping("/branches")
    public GitBranchResponse getBranches(
            @AuthenticationPrincipal Long githubId,
            @RequestParam String repositoryUrl,
            @RequestParam String branchName
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                repositoryUrl,
                branchName,
                workspace -> gitQueryService.getBranches(workspace.repositoryPath())
        );
    }

    @PutMapping("/file")
    public GitFileWriteResponse writeFile(
            @AuthenticationPrincipal Long githubId,
            @Valid @RequestBody GitFileWriteRequest request
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                request.repositoryUrl(),
                request.branchName(),
                workspace -> gitWorkingTreeService.writeFile(workspace.repositoryPath(), request)
        );
    }

    @PostMapping("/commit")
    public GitCommitResponse commit(
            @AuthenticationPrincipal Long githubId,
            @Valid @RequestBody GitCommitRequest request
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                request.repositoryUrl(),
                request.branchName(),
                workspace -> gitCommandService.commit(workspace.repositoryPath(), request)
        );
    }

    @PostMapping("/push")
    public GitPushResponse push(
            @AuthenticationPrincipal Long githubId,
            @Valid @RequestBody GitPushRequest request
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                request.repositoryUrl(),
                request.branchName(),
                workspace -> gitCommandService.push(
                        workspace.repositoryPath(), workspace.accessToken(), request
                )
        );
    }

    @PostMapping("/commit-and-push")
    public GitCommitAndPushResponse commitAndPush(
            @AuthenticationPrincipal Long githubId,
            @Valid @RequestBody GitCommitAndPushRequest request
    ) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                request.repositoryUrl(),
                request.branchName(),
                workspace -> gitCommandService.commitAndPush(
                        workspace.repositoryPath(), workspace.accessToken(), request
                )
        );
    }
}
