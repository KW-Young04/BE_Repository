package com.example.be_young04.domain.repository.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepositoryInfo {
    private final String owner;
    private final String repo;
}