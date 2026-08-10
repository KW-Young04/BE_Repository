package com.example.be_young04.domain.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RepositoryBranchRequest {
    private String repositoryUrl;
    private String branchName;
}
