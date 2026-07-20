package com.example.be_young04.domain.repository.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico",
            ".woff", ".woff2", ".ttf", ".otf",
            ".mp4", ".webm", ".mov", ".mp3", ".wav"
    );

    private final RestClient githubRestClient;
    private final GithubUrlParser githubUrlParser;

    public RepositoryTreeResponse getRepositoryTree(String repositoryUrl) {
        return getRepositoryTree(repositoryUrl, "HEAD");
    }

    public RepositoryTreeResponse getRepositoryTree(String repositoryUrl, String branchName) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);
        String ref = normalizeBranchName(branchName);

        GithubTreeResponse treeResponse = githubRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/git/trees/")
                        .pathSegment(ref)
                        .queryParam("recursive", "1")
                        .build(repositoryInfo.getOwner(), repositoryInfo.getRepo()))
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
                .branch(ref)
                .nodes(nodes)
                .build();
    }

    public RepositoryFileResponse getFileContent(String repositoryUrl, String filePath) {
        return getFileContent(repositoryUrl, filePath, "HEAD");
    }

    public RepositoryFileResponse getFileContent(String repositoryUrl, String filePath, String branchName) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);
        String ref = normalizeBranchName(branchName);

        String[] pathSegments = filePath.split("/");

        GithubContentResponse contentResponse = githubRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents")
                        .pathSegment(pathSegments)
                        .queryParam("ref", ref)
                        .build(repositoryInfo.getOwner(), repositoryInfo.getRepo()))
                .retrieve()
                .body(GithubContentResponse.class);

        if (contentResponse == null) {
            throw new IllegalStateException("파일 내용을 불러오지 못했습니다.");
        }

        String normalizedContent = normalizeBase64Content(contentResponse.getContent());
        boolean binary = isBinaryFile(contentResponse.getPath());

        return RepositoryFileResponse.builder()
                .owner(repositoryInfo.getOwner())
                .repo(repositoryInfo.getRepo())
                .branch(ref)
                .path(contentResponse.getPath())
                .content(binary ? normalizedContent : decodeBase64Content(normalizedContent))
                .encoding(binary ? "base64" : "utf-8")
                .build();
    }

    private boolean isBinaryFile(String path) {
        String lowerPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return BINARY_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }

    private String normalizeBase64Content(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content.replace("\n", "").replace("\r", "");
    }

    private String decodeBase64Content(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        byte[] decoded = Base64.getDecoder().decode(content);
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

    private String normalizeBranchName(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            return "HEAD";
        }

        return branchName.trim();
    }
}