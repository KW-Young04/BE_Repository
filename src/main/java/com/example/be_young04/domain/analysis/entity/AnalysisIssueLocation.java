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

    @Column(name = "ORIGINAL_CODE_BLOCK", nullable = false, columnDefinition = "TEXT")
    private String originalCodeBlock;

    @Column(name = "CSS_SELECTOR")
    private String cssSelector;

    @Column(name = "COMPONENT_NAME")
    private String componentName;

    @Column(name = "STATUS", nullable = false)
    private String status; // OPEN, MODIFY, COMPLETE

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public AnalysisIssueLocation(Long analysisIssueId, String targetFilePath,
                                  String originalCodeBlock, String cssSelector,
                                  String componentName) {
        this.analysisIssueId = analysisIssueId;
        this.targetFilePath = targetFilePath;
        this.originalCodeBlock = originalCodeBlock;
        this.cssSelector = cssSelector;
        this.componentName = componentName;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}