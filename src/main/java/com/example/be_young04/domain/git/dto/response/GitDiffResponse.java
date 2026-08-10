package com.example.be_young04.domain.git.dto.response;

public record GitDiffResponse(
        String path,
        String diff
) {
}
