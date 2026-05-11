package com.example.be_young04.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANALYSIS_RESULTS")
@Getter
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_RESULT_ID")
    private Long analysisResultId;

    @Column(name = "ANALYSIS_REQUEST_ID", nullable = false, unique = true)
    private Long analysisRequestId;

    @Column(name = "SUMMARY", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "OVERALL_SCORE", nullable = false)
    private int overallScore;

    @Column(name = "PASS_COUNT", nullable = false)
    private int passCount;

    @Column(name = "FAIL_COUNT", nullable = false)
    private int failCount;

    @Column(name = "NA_COUNT", nullable = false)
    private int naCount;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Builder
    public AnalysisResult(Long analysisRequestId, String summary,
                          int overallScore, int passCount, int failCount, int naCount) {
        this.analysisRequestId = analysisRequestId;
        this.summary = summary;
        this.overallScore = overallScore;
        this.passCount = passCount;
        this.failCount = failCount;
        this.naCount = naCount;
        this.createdAt = LocalDateTime.now();
    }
}