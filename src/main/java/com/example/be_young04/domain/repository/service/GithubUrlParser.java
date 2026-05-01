package com.example.be_young04.domain.repository.service;

import com.example.be_young04.domain.repository.dto.RepositoryInfo;
import com.example.be_young04.domain.repository.exception.InvalidGithubUrlException;
import org.springframework.stereotype.Component;

@Component
public class GithubUrlParser {

    public RepositoryInfo parse(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new InvalidGithubUrlException("저장소 URL이 비어 있습니다.");
        }

        String normalized = repositoryUrl.trim();

        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (!normalized.startsWith("https://github.com/")) {
            throw new InvalidGithubUrlException("올바른 GitHub 저장소 URL이 아닙니다.");
        }

        String path = normalized.replace("https://github.com/", "");
        String[] parts = path.split("/");

        if (parts.length < 2) {
            throw new InvalidGithubUrlException("저장소 URL 형식이 올바르지 않습니다.");
        }

        return RepositoryInfo.builder()
                .owner(parts[0])
                .repo(parts[1])
                .build();
    }
}