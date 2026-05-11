package com.example.be_young04.domain.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "REPOSITORIES")
@Getter
@NoArgsConstructor
public class Repository {

    @Id
    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "GITHUB_ID", nullable = false)
    private Long githubId;

    @Column(name = "OWNER_NAME", nullable = false)
    private String ownerName;

    @Column(name = "REPOSITORY_NAME", nullable = false)
    private String repositoryName;

    @Column(name = "DEFAULT_BRANCH", nullable = false)
    private String defaultBranch;

    @Column(name = "REPOSITORY_URL", nullable = false)
    private String repositoryUrl;

    @Column(name = "IS_PRIVATE", nullable = false)
    private boolean isPrivate;

    @Column(name = "LAST_SYNCED_AT")
    private LocalDateTime lastSyncedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    public Repository(Long repositoryId, Long githubId, String ownerName, String repositoryName,
                      String defaultBranch, String repositoryUrl, boolean isPrivate) {
        this.repositoryId = repositoryId;
        this.githubId = githubId;
        this.ownerName = ownerName;
        this.repositoryName = repositoryName;
        this.defaultBranch = defaultBranch;
        this.repositoryUrl = repositoryUrl;
        this.isPrivate = isPrivate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLastSyncedAt() {
        this.lastSyncedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}