package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisResponse {
    private Long analysisId;
    private Long repositoryId;
    private String status;
    private LocalDateTime createdAt;
}
