package com.example.be_young04.domain.repository.client;

import com.example.be_young04.domain.repository.dto.GithubRepositoryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GithubRepositoryClient {

    private final RestClient restClient;

    public GithubRepositoryClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public List<GithubRepositoryResponse> getRecentRepositories(String accessToken) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("sort", "updated")
                        .queryParam("direction", "desc")
                        .queryParam("per_page", 10)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}