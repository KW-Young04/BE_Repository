package com.example.be_young04.domain.repository.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.be_young04.domain.repository.client.GithubRepositoryClient;
import com.example.be_young04.domain.repository.dto.GithubContentResponse;
import com.example.be_young04.domain.repository.dto.GithubRepositoryResponse;
import com.example.be_young04.domain.repository.dto.GithubTreeResponse;
import com.example.be_young04.domain.repository.dto.RepositoryFileResponse;
import com.example.be_young04.domain.repository.dto.RepositoryInfo;
import com.example.be_young04.domain.repository.dto.RepositoryTreeResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private final RestClient githubRestClient;
    private final GithubUrlParser githubUrlParser;
    private final GithubRepositoryClient githubRepositoryClient;

    public RepositoryTreeResponse getRepositoryTree(String repositoryUrl) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);

        GithubTreeResponse treeResponse = githubRestClient.get()
                .uri("/repos/{owner}/{repo}/git/trees/HEAD?recursive=1",
                        repositoryInfo.getOwner(), repositoryInfo.getRepo())
                .retrieve()
                .body(GithubTreeResponse.class);

        if (treeResponse == null || treeResponse.getTree() == null) {
            throw new IllegalStateException("저장소 트리 구조를 불러오지 못했습니다.");
        }

        List<RepositoryTreeResponse.TreeNode> nodes = treeResponse.getTree().stream()
                .map(item -> RepositoryTreeResponse.TreeNode.builder()
                        .path(item.getPath())
                        .type(item.getType())
                        .size(item.getSize())
                        .build())
                .toList();

        return RepositoryTreeResponse.builder()
                .owner(repositoryInfo.getOwner())
                .repo(repositoryInfo.getRepo())
                .nodes(nodes)
                .build();
    }

    public RepositoryFileResponse getFileContent(String repositoryUrl, String filePath) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);

        String[] pathSegments = filePath.split("/");

        GithubContentResponse contentResponse = githubRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents")
                        .pathSegment(pathSegments)
                        .build(repositoryInfo.getOwner(), repositoryInfo.getRepo()))
                .retrieve()
                .body(GithubContentResponse.class);

        if (contentResponse == null) {
            throw new IllegalStateException("파일 내용을 불러오지 못했습니다.");
        }

        String decodedContent = decodeBase64Content(contentResponse.getContent());

        return RepositoryFileResponse.builder()
                .owner(repositoryInfo.getOwner())
                .repo(repositoryInfo.getRepo())
                .path(contentResponse.getPath())
                .content(decodedContent)
                .build();
    }

    public List<GithubRepositoryResponse> getRecentRepositories(String accessToken) {

        return githubRepositoryClient.getRecentRepositories(accessToken);

    }

    private String decodeBase64Content(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public GithubRepositoryResponse getRepositoryInfo(String repositoryUrl) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);

        GithubRepositoryResponse response = githubRestClient.get()
                .uri("/repos/{owner}/{repo}", repositoryInfo.getOwner(), repositoryInfo.getRepo())
                .retrieve()
                .body(GithubRepositoryResponse.class);

        if (response == null) {
            throw new IllegalStateException("저장소 정보를 불러오지 못했습니다.");
        }

        return response;
    }
}