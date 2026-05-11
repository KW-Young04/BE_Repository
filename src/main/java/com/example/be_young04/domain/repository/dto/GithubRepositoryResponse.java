package com.example.be_young04.domain.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class GithubRepositoryResponse {

    private Long id;

    @JsonProperty("default_branch")
    private String defaultBranch;

    @JsonProperty("private")
    private boolean privateRepo;
}