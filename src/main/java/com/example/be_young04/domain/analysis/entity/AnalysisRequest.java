package com.example.be_young04.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANALYSIS_REQUESTS")
@Getter
@NoArgsConstructor
public class AnalysisRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_REQUEST_ID")
    private Long analysisRequestId;

    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "BRANCH_NAME", nullable = false)
    private String branchName;

    @Column(name = "TARGET_PATH", nullable = false)
    private String targetPath;

    @Column(name = "STATUS", nullable = false)
    private String status; // PENDING, RUNNING, COMPLETE, FAIL

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public AnalysisRequest(Long repositoryId, String branchName, String targetPath, String status) {
        this.repositoryId = repositoryId;
        this.branchName = branchName;
        this.targetPath = targetPath;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}