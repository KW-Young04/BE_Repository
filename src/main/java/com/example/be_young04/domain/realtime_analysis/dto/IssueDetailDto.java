package com.example.be_young04.domain.realtime_analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueDetailDto {
    private Long wcagItemId;
    private String sc;
    private String title;
    private String levelType;
    private String description;
    private String status; // "FAIL"
    private String targetFilePath;
    private String targetSelector;
    private String originalCodeBlock;
    private String suggestion;
    private String measuredValue;
    private String thresholdValue;
}