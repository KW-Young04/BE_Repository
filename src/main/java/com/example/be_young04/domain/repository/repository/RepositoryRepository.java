package com.example.be_young04.domain.repository.repository;

import com.example.be_young04.domain.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    Optional<Repository> findByGithubIdAndOwnerNameAndRepositoryName(
            Long githubId, String ownerName, String repositoryName);
}