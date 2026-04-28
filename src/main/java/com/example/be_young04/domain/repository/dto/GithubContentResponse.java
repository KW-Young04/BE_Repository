package com.example.be_young04.domain.repository.dto;

import lombok.Getter;

@Getter
public class GithubContentResponse {
    private String name;
    private String path;
    private String sha;
    private Long size;
    private String url;
    private String html_url;
    private String git_url;
    private String download_url;
    private String type;
    private String content;
    private String encoding;
}