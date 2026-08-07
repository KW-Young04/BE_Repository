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
import com.example.be_young04.domain.user.service.GithubUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private final RestClient githubRestClient;
    private final GithubUrlParser githubUrlParser;
    private final GithubRepositoryClient githubRepositoryClient;
    private final GithubUserService githubUserService;

    public RepositoryTreeResponse getRepositoryTree(Long githubId, String repositoryUrl) {
        return getRepositoryTree(githubId, repositoryUrl, "HEAD");
    }

    public RepositoryTreeResponse getRepositoryTree(Long githubId, String repositoryUrl, String branchName) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);
        String ref = normalizeBranchName(branchName);
        String accessToken = getAccessToken(githubId);

        GithubTreeResponse treeResponse = githubRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/git/trees/")
                        .pathSegment(ref)
                        .queryParam("recursive", "1")
                        .build(repositoryInfo.getOwner(), repositoryInfo.getRepo()))
                .header("Authorization", "Bearer " + accessToken)
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

    public RepositoryFileResponse getFileContent(Long githubId, String repositoryUrl, String filePath) {
        return getFileContent(githubId, repositoryUrl, filePath, "HEAD");
    }

    public RepositoryFileResponse getFileContent(
            Long githubId,
            String repositoryUrl,
            String filePath,
            String branchName
    ) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);
        String ref = normalizeBranchName(branchName);
        String accessToken = getAccessToken(githubId);

        String[] pathSegments = filePath.split("/");

        GithubContentResponse contentResponse = githubRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents")
                        .pathSegment(pathSegments)
                        .queryParam("ref", ref)
                        .build(repositoryInfo.getOwner(), repositoryInfo.getRepo()))
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GithubContentResponse.class);

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

    public List<GithubRepositoryResponse> getRecentRepositories(Long githubId) {
        return githubRepositoryClient.getRecentRepositories(getAccessToken(githubId));
    }

    private String decodeBase64Content(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public GithubRepositoryResponse getRepositoryInfo(Long githubId, String repositoryUrl) {
        RepositoryInfo repositoryInfo = githubUrlParser.parse(repositoryUrl);
        String accessToken = getAccessToken(githubId);

        GithubRepositoryResponse response = githubRestClient.get()
                .uri("/repos/{owner}/{repo}", repositoryInfo.getOwner(), repositoryInfo.getRepo())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GithubRepositoryResponse.class);

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

    private String getAccessToken(Long githubId) {
        if (githubId == null) {
            throw new IllegalStateException("로그인한 GitHub 사용자를 확인할 수 없습니다.");
        }

        return githubUserService.getById(githubId).getAccessToken();
    }
}
