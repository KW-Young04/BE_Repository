package com.example.be_young04.domain.user.repository;

import com.example.be_young04.domain.user.entity.GithubUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubUserRepository extends JpaRepository<GithubUser, Long> {
    Optional<GithubUser> findByUsername(String username);
}