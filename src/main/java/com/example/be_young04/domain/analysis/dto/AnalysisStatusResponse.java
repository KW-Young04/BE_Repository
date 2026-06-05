package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisStatusResponse {
    private Long analysisId;
    private String status;
    private Integer progressPercent;
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime estimatedCompletedAt;
}
