package com.example.be_young04.domain.repository.service;

import com.example.be_young04.domain.repository.dto.GithubRepositoryResponse;
import com.example.be_young04.domain.repository.entity.Repository;
import com.example.be_young04.domain.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DBRepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final GithubRepositoryService githubRepositoryService;
    private final GithubUrlParser githubUrlParser;

    @Transactional
    public Repository getOrCreate(Long githubId, String repositoryUrl) {
        return repositoryRepository.findByRepositoryUrl(repositoryUrl)
            .orElseGet(() -> {
                GithubRepositoryResponse info = githubRepositoryService
                        .getRepositoryInfo(repositoryUrl);
                var repoInfo = githubUrlParser.parse(repositoryUrl);

                Repository newRepo = Repository.builder()
                        .repositoryId(info.getId())
                        .githubId(githubId)
                        .ownerName(repoInfo.getOwner())
                        .repositoryName(repoInfo.getRepo())
                        .defaultBranch(info.getDefaultBranch())
                        .repositoryUrl(repositoryUrl)
                        .isPrivate(info.isPrivateRepo())
                        .build();

                return repositoryRepository.save(newRepo);
            });
    }
}