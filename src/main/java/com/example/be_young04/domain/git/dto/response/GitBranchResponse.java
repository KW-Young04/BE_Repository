package com.example.be_young04.domain.git.dto.response;

import java.util.List;

public record GitBranchResponse(
        String currentBranch,
        List<String> branches
) {
}