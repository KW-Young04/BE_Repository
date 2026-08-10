package com.example.be_young04.domain.git.dto.response;

public record GitCommitAndPushResponse(
        GitOperationResult commit,
        GitOperationResult push
) {
    public record GitOperationResult(
            boolean success,
            String code,
            String message,
            String commitHash
    ) {
    }
}