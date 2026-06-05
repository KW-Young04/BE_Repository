package com.example.be_young04.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * WCAG 2.2 항목 정의
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "wcag_items")
public class WcagItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wcagItemId;

    @Column(nullable = false, unique = true)
    private String successCriteria; // SC 번호 (예: 1.1.1)

    @Column(nullable = false)
    private String title; // 항목명

    @Column(nullable = false)
    private String levelType; // A, AA, AAA

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 항목 설명

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
