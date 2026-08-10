package com.example.be_young04.domain.git.service.model;

public record GitCommandResult(
        int exitCode,
        String stdout,
        String stderr
) {
    public boolean isSuccess() {
        return exitCode == 0;
    }
}