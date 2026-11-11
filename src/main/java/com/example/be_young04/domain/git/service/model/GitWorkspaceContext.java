package com.example.be_young04.domain.git.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;

public final class GitWorkspaceContext {

    private final Path repositoryPath;
    private final String branch;
    private final String accessToken;

    public GitWorkspaceContext(Path repositoryPath, String branch, String accessToken) {
        this.repositoryPath = repositoryPath;
        this.branch = branch;
        this.accessToken = accessToken;
    }

    public Path repositoryPath() {
        return repositoryPath;
    }

    public String branch() {
        return branch;
    }

    @JsonIgnore
    public String accessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "GitWorkspaceContext[repositoryPath=%s, branch=%s, accessToken=***]"
                .formatted(repositoryPath, branch);
    }
}
