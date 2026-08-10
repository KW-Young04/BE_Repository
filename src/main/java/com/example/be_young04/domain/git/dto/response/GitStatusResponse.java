package com.example.be_young04.domain.git.dto.response;

import java.util.List;

public record GitStatusResponse(
        String branch,
        boolean hasChanges,
        List<GitFileChangeResponse> files
) {
}