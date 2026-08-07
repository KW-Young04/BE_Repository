package com.example.be_young04.domain.git.dto.response;

public record GitCommitResponse(
        boolean success,
        String commitHash,
        String message,
        int changedFileCount
) {
}
