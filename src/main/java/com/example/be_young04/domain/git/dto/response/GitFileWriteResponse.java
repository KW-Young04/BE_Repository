package com.example.be_young04.domain.git.dto.response;

public record GitFileWriteResponse(
        boolean success,
        String path
) {
}
