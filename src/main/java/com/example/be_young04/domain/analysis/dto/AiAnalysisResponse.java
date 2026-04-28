package com.example.be_young04.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiAnalysisResponse {
    private CodeAnalysisResult codeParsingResult;
    private String snapshotPath;
    private String aiReport;
}