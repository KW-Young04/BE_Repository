package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisRequest {
    private Long repositoryId;
    private String deploymentUrl;
    private List<String> filePathList;
}
