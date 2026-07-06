package com.example.be_young04.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubClientConfig {

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public RestClient githubRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .build();
    }
}