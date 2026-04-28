package com.example.be_young04.domain.repository.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RepositoryTreeResponse {
    private String owner;
    private String repo;
    private List<TreeNode> nodes;

    @Getter
    @Builder
    public static class TreeNode {
        private String path;
        private String type;
        private Long size;
    }
}