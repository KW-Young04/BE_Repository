package com.example.be_young04.domain.repository.service;

import com.example.be_young04.domain.repository.dto.GithubRepositoryResponse;
import com.example.be_young04.domain.repository.dto.RecentRepositoryResponse;
import com.example.be_young04.domain.repository.entity.Repository;
import com.example.be_young04.domain.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DBRepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final GithubRepositoryService githubRepositoryService;
    private final GithubUrlParser githubUrlParser;

    @Transactional
    public Repository getOrCreate(Long githubId, String repositoryUrl) {
        var repoInfo = githubUrlParser.parse(repositoryUrl);

        return repositoryRepository
                .findByGithubIdAndOwnerNameAndRepositoryName(
                        githubId, repoInfo.getOwner(), repoInfo.getRepo())
                .orElseGet(() -> {
                    GithubRepositoryResponse info = githubRepositoryService
                            .getRepositoryInfo(repositoryUrl);

                    Repository newRepo = Repository.builder()
                            .repositoryId(info.getId())
                            .githubId(githubId)
                            .ownerName(repoInfo.getOwner())
                            .repositoryName(repoInfo.getRepo())
                            .defaultBranch(info.getDefaultBranch())
                            .isPrivate(info.isPrivateRepo())
                            .build();

                    return repositoryRepository.save(newRepo);
                });
    }

    @Transactional(readOnly = true)
    public List<RecentRepositoryResponse> getRecentRepositories(Long githubId) {
        return repositoryRepository.findByGithubIdOrderByUpdatedAtDesc(githubId).stream()
                .limit(10)
                .map(RecentRepositoryResponse::from)
                .toList();
    }
}