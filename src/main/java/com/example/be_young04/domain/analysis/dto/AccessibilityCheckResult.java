package com.example.be_young04.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class AccessibilityCheckResult {
    private String successCriteria;
    private String name;
    private String level;
    private String mvpDescription;
    private String implementationMethod;
    private String implementationDescription;
    private String status;
    private boolean aiReviewRequired;
    private String aiReviewGuide;
    private String aiReviewStatus;
    private String aiReviewResult;
    private String defaultSuggestion;
    private List<AccessibilityIssue> issues;
}
