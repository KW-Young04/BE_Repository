package com.example.be_young04.domain.repository.service;

import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".bmp",
            ".woff", ".woff2", ".ttf", ".otf", ".eot",
            ".pdf", ".zip", ".gz", ".tar", ".mp4", ".webm", ".mp3", ".wav"
    );

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
                .truncated(Boolean.TRUE.equals(treeResponse.getTruncated()))
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

        String safeFilePath = validateRepositoryFilePath(filePath);
        String[] pathSegments = safeFilePath.split("/");

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

        DecodedFileContent decoded = decodeGithubContent(
                contentResponse.getContent(),
                contentResponse.getPath()
        );

        return RepositoryFileResponse.builder()
                .owner(repositoryInfo.getOwner())
                .repo(repositoryInfo.getRepo())
                .branch(ref)
                .path(contentResponse.getPath())
                .content(decoded.content())
                .encoding(decoded.encoding())
                .build();
    }

    public List<GithubRepositoryResponse> getRecentRepositories(Long githubId) {
        return githubRepositoryClient.getRecentRepositories(getAccessToken(githubId));
    }

    private DecodedFileContent decodeGithubContent(String content, String path) {
        if (content == null || content.isBlank()) {
            return new DecodedFileContent("", "utf-8");
        }

        String normalized = content.replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        if (isBinaryPath(path)) {
            return new DecodedFileContent(normalized, "base64");
        }

        try {
            String text = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(decoded))
                    .toString();
            return new DecodedFileContent(text, "utf-8");
        } catch (CharacterCodingException e) {
            return new DecodedFileContent(normalized, "base64");
        }
    }

    private boolean isBinaryPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return BINARY_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private String validateRepositoryFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("파일 경로가 비어 있습니다.");
        }

        String normalized = filePath.trim();
        if (normalized.length() > 1024 || normalized.contains("\\") || normalized.contains("\0")) {
            throw new IllegalArgumentException("파일 경로 형식이 올바르지 않습니다.");
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("절대 경로는 사용할 수 없습니다.");
        }

        for (String segment : normalized.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("파일 경로에 허용되지 않는 구간이 포함되어 있습니다.");
            }
        }

        return normalized;
    }

    private record DecodedFileContent(String content, String encoding) {
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
