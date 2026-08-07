package com.example.be_young04.domain.git.dto.response;

public record GitErrorResponse(
        boolean success,
        String code,
        String message
) {
}
