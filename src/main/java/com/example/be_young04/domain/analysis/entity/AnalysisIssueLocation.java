package com.example.be_young04.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANALYSIS_ISSUES_LOCATIONS")
@Getter
@NoArgsConstructor
public class AnalysisIssueLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_ISSUE_LOCATION_ID")
    private Long analysisIssueLocationId;

    @Column(name = "ANALYSIS_ISSUE_ID", nullable = false)
    private Long analysisIssueId;

    @Column(name = "TARGET_FILE_PATH", nullable = false)
    private String targetFilePath;

    @Column(name = "TARGET_SELECTOR")
    private String targetSelector;

    @Column(name = "ORIGINAL_CODE_BLOCK", nullable = false, columnDefinition = "TEXT")
    private String originalCodeBlock;

    @Column(name = "SUGGESTION", nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "MEASURED_VALUE", length = 50)
    private String measuredValue;

    @Column(name = "THRESHOLD_VALUE", length = 50)
    private String thresholdValue;

    @Column(name = "SUGGESTION_TYPE")
    private String suggestionType; // COLOR_CONTRAST, CODE_FIX, ATTRIBUTE, LAYOUT, TEXT

    @Column(name = "SUGGESTION_DETAIL", columnDefinition = "JSON")
    private String suggestionDetail;

    @Column(name = "STATUS", nullable = false)
    private String status; // OPEN, MODIFY, COMPLETE

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public AnalysisIssueLocation(Long analysisIssueId, String targetFilePath, String targetSelector,
                                  String originalCodeBlock, String suggestion, String measuredValue,
                                  String thresholdValue, String suggestionType, String suggestionDetail) {
        this.analysisIssueId = analysisIssueId;
        this.targetFilePath = targetFilePath;
        this.targetSelector = targetSelector;
        this.originalCodeBlock = originalCodeBlock;
        this.suggestion = suggestion;
        this.measuredValue = measuredValue;
        this.thresholdValue = thresholdValue;
        this.suggestionType = suggestionType;
        this.suggestionDetail = suggestionDetail;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}