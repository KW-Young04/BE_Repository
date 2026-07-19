package com.example.be_young04.domain.repository.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepositoryFileResponse {
    private String owner;
    private String repo;
    private String branch;
    private String path;
    private String content;
}
