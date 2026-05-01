package com.example.be_young04.domain.repository.dto;

import lombok.Getter;

@Getter
public class GithubTreeItem {
    private String path;
    private String mode;
    private String type;
    private String sha;
    private Long size;
    private String url;
}