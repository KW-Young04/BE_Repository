package com.example.be_young04.domain.user.service;

import com.example.be_young04.domain.user.entity.GithubUser;
import com.example.be_young04.domain.user.repository.GithubUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GithubUserService {

    private final GithubUserRepository githubUserRepository;

    public GithubUser saveOrUpdate(Long githubId, String username, String profileImageUrl, String accessToken) {
        return githubUserRepository.findById(githubId)
                .map(user -> {
                    user.updateToken(accessToken);
                    return githubUserRepository.save(user);
                })
                .orElseGet(() -> githubUserRepository.save(
                        GithubUser.builder()
                                .githubId(githubId)
                                .username(username)
                                .profileImageUrl(profileImageUrl)
                                .accessToken(accessToken)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }

    public GithubUser getById(Long githubId) {
        return githubUserRepository.findById(githubId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}