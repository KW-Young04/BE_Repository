package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisResultResponse {
    private Long analysisId;
    private Long repositoryId;
    private Long totalIssues;
    private Long passCount;
    private Long failCount;
    private Double complianceRate;
    private List<WcagResultDto> wcagResults;
    private LocalDateTime completedAt;
}
