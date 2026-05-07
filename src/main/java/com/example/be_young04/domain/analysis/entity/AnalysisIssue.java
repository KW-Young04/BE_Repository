package com.example.be_young04.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANALYSIS_ISSUES")
@Getter
@NoArgsConstructor
public class AnalysisIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_ISSUE_ID")
    private Long analysisIssueId;

    @Column(name = "ANALYSIS_RESULT_ID", nullable = false)
    private Long analysisResultId;

    @Column(name = "ISSUE_TYPE", nullable = false)
    private String issueType;

    @Column(name = "LEVEL_TYPE", nullable = false)
    private String levelType;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "DESCRIPTION", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "SUGGESTION", nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "STATUS", nullable = false)
    private String status; // OPEN, PROGRESS, COMPLETE, IGNORE

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public AnalysisIssue(Long analysisResultId, String issueType, String levelType,
                         String title, String description, String suggestion) {
        this.analysisResultId = analysisResultId;
        this.issueType = issueType;
        this.levelType = levelType;
        this.title = title;
        this.description = description;
        this.suggestion = suggestion;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}