package com.example.be_young04.domain.repository.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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

    @Value("${github.token:}")
    private String githubToken;

    public RepositoryTreeResponse getRepositoryTree(String repositoryUrl) {
        return getRepositoryTree(repositoryUrl, "HEAD");
    }

    public RepositoryTreeResponse getRepositoryTree(String repositoryUrl, String branchName) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);
        String ref = normalizeBranchName(branchName);

        GithubTreeResponse treeResponse = executeGitHubRequest(
                GithubTreeResponse.class,
                "/repos/{owner}/{repo}/git/trees/{ref}?recursive=1",
                repositoryInfo.getOwner(),
                repositoryInfo.getRepo(),
                ref);

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

        GithubContentResponse contentResponse = executeGitHubRequest(
                GithubContentResponse.class,
                "/repos/{owner}/{repo}/contents/{path}",
                repositoryInfo.getOwner(),
                repositoryInfo.getRepo(),
                String.join("/", pathSegments),
                ref);

        if (contentResponse == null) {
            throw new IllegalStateException("파일 내용을 불러오지 못했습니다.");
        }

        String decodedContent = decodeBase64Content(contentResponse.getContent());

        return RepositoryFileResponse.builder()
                .owner(repositoryInfo.getOwner())
                .repo(repositoryInfo.getRepo())
                .branch(ref)
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

        GithubRepositoryResponse response = executeGitHubRequest(
                GithubRepositoryResponse.class,
                "/repos/{owner}/{repo}",
                repositoryInfo.getOwner(),
                repositoryInfo.getRepo());

        if (response == null) {
            throw new IllegalStateException("저장소 정보를 불러오지 못했습니다.");
        }

        return response;
    }

    private <T> T executeGitHubRequest(Class<T> responseType, String uriTemplate, Object... uriVariables) {
        try {
            RestClient.RequestHeadersSpec<?> request = githubRestClient.get().uri(uriTemplate, uriVariables);
            if (StringUtils.hasText(githubToken)) {
                request.header("Authorization", "Bearer " + githubToken);
            }
            return request.retrieve().body(responseType);
        } catch (RestClientResponseException e) {
            if (!isAuthFailure(e) || !StringUtils.hasText(githubToken)) {
                throw e;
            }

            try {
                return githubRestClient.get()
                        .uri(uriTemplate, uriVariables)
                        .retrieve()
                        .body(responseType);
            } catch (RestClientResponseException retryException) {
                throw new IllegalStateException(
                        "GitHub 저장소를 찾을 수 없습니다. URL과 브랜치 이름을 확인해 주세요.",
                        retryException);
            }
        }
    }

    private boolean isAuthFailure(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403;
    }

    private String normalizeBranchName(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            return "HEAD";
        }

        return branchName.trim();
    }
}
