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

    @Column(name = "ANALYSIS_WCAG_RESULT_ID", nullable = false)
    private Long analysisWcagResultId;

    @Column(name = "STATUS", nullable = false)
    private String status; // OPEN, PROGRESS, COMPLETE, IGNORE

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public AnalysisIssue(Long analysisWcagResultId) {
        this.analysisWcagResultId = analysisWcagResultId;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}