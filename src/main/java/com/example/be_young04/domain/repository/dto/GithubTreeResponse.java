package com.example.be_young04.domain.repository.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class GithubTreeResponse {
    private String sha;
    private Boolean truncated;
    private List<GithubTreeItem> tree;
}