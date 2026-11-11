package com.example.be_young04.domain.git.dto.response;

import java.util.List;

public record GitGraphResponse(
        String currentBranch,
        List<GitCommitLogResponse> commits
) {
}
