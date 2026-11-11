package com.example.be_young04.domain.git.dto.response;

import java.util.List;

public record GitCommitLogResponse(
        String hash,
        String shortHash,
        String author,
        String message,
        List<String> refs,
        boolean head
) {
}
