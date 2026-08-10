package com.example.be_young04.domain.git.dto.response;

public record GitPushResponse(
        boolean success,
        String remote,
        String branch
) {
}