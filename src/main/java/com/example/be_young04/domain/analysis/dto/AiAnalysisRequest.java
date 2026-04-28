package com.example.be_young04.domain.analysis.dto;

import lombok.Getter;

@Getter
public class AiAnalysisRequest {
    private String repositoryUrl;
    private String deploymentUrl;
    private String fileName;
    private String code;
}