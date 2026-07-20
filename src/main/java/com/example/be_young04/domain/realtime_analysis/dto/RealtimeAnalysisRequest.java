package com.example.be_young04.domain.realtime_analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RealtimeAnalysisRequest {
    private String code;
    private String targetFilePath;
}
