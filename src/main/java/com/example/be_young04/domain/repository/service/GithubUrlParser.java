package com.example.be_young04.domain.repository.service;

import com.example.be_young04.domain.repository.dto.RepositoryInfo;
import com.example.be_young04.domain.repository.exception.InvalidGithubUrlException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class GithubUrlParser {

    private static final Pattern OWNER_PATTERN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?");
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

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

        String path = normalized.substring("https://github.com/".length());
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }

        String[] parts = path.split("/", -1);
        if (parts.length != 2
                || !OWNER_PATTERN.matcher(parts[0]).matches()
                || !REPOSITORY_PATTERN.matcher(parts[1]).matches()) {
            throw new InvalidGithubUrlException("저장소 URL 형식이 올바르지 않습니다.");
        }

        return RepositoryInfo.builder()
                .owner(parts[0])
                .repo(parts[1])
                .build();
    }
}
