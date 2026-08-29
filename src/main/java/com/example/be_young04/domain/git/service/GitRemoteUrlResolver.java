package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.repository.dto.RepositoryInfo;
import org.springframework.stereotype.Component;

@Component
public class GitRemoteUrlResolver {

    public String resolve(RepositoryInfo repositoryInfo) {
        return "https://github.com/%s/%s.git".formatted(
                repositoryInfo.getOwner(),
                repositoryInfo.getRepo()
        );
    }
}
