package com.example.be_young04.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANALYSIS_WCAG_RESULTS")
@Getter
@NoArgsConstructor
public class AnalysisWcagResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_WCAG_RESULT_ID")
    private Long analysisWcagResultId;

    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "WCAG_ITEM_ID", nullable = false)
    private Long wcagItemId;

    @Column(name = "STATUS", nullable = false)
    private String status; // PASS, FAIL, NA

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public AnalysisWcagResult(Long repositoryId, Long wcagItemId, String status) {
        this.repositoryId = repositoryId;
        this.wcagItemId = wcagItemId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}