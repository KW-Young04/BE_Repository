package com.example.be_young04.domain.analysis.entity;

import com.example.be_young04.domain.repository.entity.Repository;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * WCAG 항목별 분석 결과 (PASS/FAIL/NA)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "analysis_wcag_results")
public class AnalysisWcagResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisWcagResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wcag_item_id", nullable = false)
    private WcagItem wcagItem;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status; // PASS, FAIL, NA

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AnalysisStatus {
        PASS, FAIL, NA
    }
}
