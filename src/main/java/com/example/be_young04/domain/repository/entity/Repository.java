package com.example.be_young04.domain.repository.entity;

import com.example.be_young04.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자가 연결한 GitHub Repository 정보
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "repositories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"github_id", "owner_name", "repository_name"})
})
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long repositoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String repositoryName;

    @Column(nullable = false)
    private Boolean isPrivate = false;

    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getRepositoryUrl() {
        return String.format("https://github.com/%s/%s", ownerName, repositoryName);
    }
}
