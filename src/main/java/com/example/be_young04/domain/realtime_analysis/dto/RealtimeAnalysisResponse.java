package com.example.be_young04.domain.realtime_analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RealtimeAnalysisResponse {
    private boolean success;
    private LocalDateTime timestamp;
    private int issueCount;
    private List<IssueDetailDto> issues;
}