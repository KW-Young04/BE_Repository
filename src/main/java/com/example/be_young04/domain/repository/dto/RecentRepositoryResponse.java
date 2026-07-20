package com.example.be_young04.domain.repository.dto;

import com.example.be_young04.domain.repository.entity.Repository;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentRepositoryResponse {

    private Long id;
    private String name;
    private String fullName;
    private String htmlUrl;
    private boolean privateRepo;
    private String language;
    private LocalDateTime updatedAt;
    private String defaultBranch;

    public static RecentRepositoryResponse from(Repository repository) {
        String fullName = repository.getOwnerName() + "/" + repository.getRepositoryName();
        return RecentRepositoryResponse.builder()
                .id(repository.getRepositoryId())
                .name(repository.getRepositoryName())
                .fullName(fullName)
                .htmlUrl("https://github.com/" + fullName)
                .privateRepo(repository.isPrivate())
                .language(null)
                .updatedAt(repository.getUpdatedAt())
                .defaultBranch(repository.getDefaultBranch())
                .build();
    }
}