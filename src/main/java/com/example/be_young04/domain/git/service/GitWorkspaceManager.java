package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.config.GitProperties;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.service.model.GitCommandResult;
import com.example.be_young04.domain.git.service.model.GitWorkspaceContext;
import com.example.be_young04.domain.git.type.GitErrorCode;
import com.example.be_young04.domain.repository.dto.RepositoryInfo;
import com.example.be_young04.domain.repository.exception.InvalidGithubUrlException;
import com.example.be_young04.domain.repository.service.GithubUrlParser;
import com.example.be_young04.domain.user.entity.GithubUser;
import com.example.be_young04.domain.user.service.GithubUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GitWorkspaceManager {

    private static final Pattern INVALID_BRANCH_CHARACTER = Pattern.compile("[\\p{Cntrl} ~^:?*\\[\\\\]");

    private final GitProperties gitProperties;
    private final GitCommandExecutor gitCommandExecutor;
    private final GithubUrlParser githubUrlParser;
    private final GithubUserService githubUserService;
    private final GitRemoteUrlResolver gitRemoteUrlResolver;
    private final ConcurrentMap<Path, ReentrantLock> workspaceLocks = new ConcurrentHashMap<>();

    public <T> T withWorkspace(
            Long githubId,
            String repositoryUrl,
            String requestedBranch,
            Function<GitWorkspaceContext, T> operation
    ) {
        WorkspaceRequest request = createWorkspaceRequest(githubId, repositoryUrl, requestedBranch);
        ReentrantLock lock = workspaceLocks.computeIfAbsent(
                request.workspacePath(), ignored -> new ReentrantLock()
        );

        lock.lock();
        try {
            GitWorkspaceContext context = prepareWorkspace(request);
            return operation.apply(context);
        } finally {
            lock.unlock();
        }
    }

    private WorkspaceRequest createWorkspaceRequest(
            Long githubId,
            String repositoryUrl,
            String requestedBranch
    ) {
        if (githubId == null) {
            throw new GitOperationException(GitErrorCode.AUTHENTICATION_FAILED);
        }

        RepositoryInfo repositoryInfo;
        try {
            repositoryInfo = githubUrlParser.parse(repositoryUrl);
        } catch (InvalidGithubUrlException exception) {
            throw new GitOperationException(GitErrorCode.INVALID_REPOSITORY_URL, exception);
        }

        String branch = normalizeBranch(requestedBranch);
        Path workspaceRoot = getWorkspaceRoot();
        String workspaceKey = hashWorkspaceKey(githubId, repositoryInfo, branch);
        Path userRoot = workspaceRoot.resolve(githubId.toString()).normalize();
        Path workspacePath = userRoot.resolve(workspaceKey).normalize();

        if (!userRoot.startsWith(workspaceRoot) || !workspacePath.startsWith(userRoot)) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }

        GithubUser githubUser;
        try {
            githubUser = githubUserService.getById(githubId);
        } catch (RuntimeException exception) {
            throw new GitOperationException(GitErrorCode.AUTHENTICATION_FAILED, exception);
        }

        if (githubUser.getAccessToken() == null || githubUser.getAccessToken().isBlank()) {
            throw new GitOperationException(GitErrorCode.AUTHENTICATION_FAILED);
        }

        String username = normalizeUsername(githubId, githubUser.getUsername());

        return new WorkspaceRequest(
                userRoot,
                workspacePath,
                gitRemoteUrlResolver.resolve(repositoryInfo),
                branch,
                githubUser.getAccessToken(),
                username,
                githubId + "+" + username + "@users.noreply.github.com"
        );
    }

    private GitWorkspaceContext prepareWorkspace(WorkspaceRequest request) {
        if (!Files.exists(request.workspacePath())) {
            cloneRepository(request);
        }

        validateWorkspace(request);
        configureCommitIdentity(request);
        String currentBranch = getCurrentBranch(request.workspacePath());

        if (request.branch() != null && !request.branch().equals(currentBranch)) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }

        return new GitWorkspaceContext(
                request.workspacePath(),
                currentBranch,
                request.accessToken()
        );
    }

    private void cloneRepository(WorkspaceRequest request) {
        Path temporaryClone = null;
        try {
            Files.createDirectories(request.userRoot());
            temporaryClone = Files.createTempDirectory(request.userRoot(), ".clone-");

            List<String> arguments = new ArrayList<>();
            arguments.add("clone");
            arguments.add("--origin");
            arguments.add(gitProperties.getDefaultRemote());
            if (request.branch() != null) {
                arguments.add("--branch");
                arguments.add(request.branch());
                arguments.add("--single-branch");
            }
            arguments.add(request.cloneUrl());
            arguments.add(temporaryClone.toString());

            GitCommandResult cloneResult = gitCommandExecutor.executeAuthenticated(
                    request.userRoot(),
                    request.accessToken(),
                    arguments.toArray(String[]::new)
            );
            if (!cloneResult.isSuccess() || !Files.isDirectory(temporaryClone.resolve(".git"))) {
                throw classifyCloneFailure(cloneResult);
            }

            boolean moved = moveWorkspace(temporaryClone, request.workspacePath());
            if (moved) {
                temporaryClone = null;
            }
        } catch (GitOperationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED, exception);
        } finally {
            deleteTemporaryClone(temporaryClone, request.userRoot());
        }
    }

    private boolean moveWorkspace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
            return true;
        } catch (FileAlreadyExistsException exception) {
            if (!Files.isDirectory(destination.resolve(".git"))) {
                throw exception;
            }
            return false;
        }
    }

    private void validateWorkspace(WorkspaceRequest request) {
        if (!Files.isDirectory(request.workspacePath())
                || !Files.isDirectory(request.workspacePath().resolve(".git"))) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }

        try {
            if (!request.workspacePath().toRealPath().startsWith(request.userRoot().toRealPath())) {
                throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
            }
        } catch (IOException exception) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED, exception);
        }

        GitCommandResult remoteResult = gitCommandExecutor.execute(
                request.workspacePath(),
                "remote", "get-url", gitProperties.getDefaultRemote()
        );
        if (!remoteResult.isSuccess()
                || !normalizeRemoteUrl(remoteResult.stdout()).equals(normalizeRemoteUrl(request.cloneUrl()))) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }
    }

    private String getCurrentBranch(Path workspacePath) {
        GitCommandResult branchResult = gitCommandExecutor.execute(
                workspacePath, "branch", "--show-current"
        );
        if (!branchResult.isSuccess() || branchResult.stdout().isBlank()) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }
        return branchResult.stdout().trim();
    }

    private void configureCommitIdentity(WorkspaceRequest request) {
        GitCommandResult nameResult = gitCommandExecutor.execute(
                request.workspacePath(), "config", "user.name", request.username()
        );
        GitCommandResult emailResult = gitCommandExecutor.execute(
                request.workspacePath(), "config", "user.email", request.email()
        );
        if (!nameResult.isSuccess() || !emailResult.isSuccess()) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }
    }

    private GitOperationException classifyCloneFailure(GitCommandResult result) {
        String error = result.stderr().isBlank() ? result.stdout() : result.stderr();
        String normalized = error.toLowerCase(Locale.ROOT);
        if (normalized.contains("authentication failed")
                || normalized.contains("permission denied")
                || normalized.contains("repository not found")
                || normalized.contains("403")) {
            return new GitOperationException(GitErrorCode.AUTHENTICATION_FAILED, error);
        }
        return new GitOperationException(GitErrorCode.CLONE_FAILED, error);
    }

    private String normalizeBranch(String requestedBranch) {
        if (requestedBranch == null || requestedBranch.isBlank()) {
            throw new GitOperationException(GitErrorCode.INVALID_BRANCH);
        }

        String branch = requestedBranch.trim();
        boolean invalid = branch.length() > 255
                || branch.equals("@")
                || branch.startsWith("-")
                || branch.startsWith(".")
                || branch.endsWith(".")
                || branch.endsWith("/")
                || branch.contains("..")
                || branch.contains("@{")
                || INVALID_BRANCH_CHARACTER.matcher(branch).find();

        if (!invalid) {
            for (String part : branch.split("/", -1)) {
                if (part.isEmpty() || part.equals(".") || part.equals("..") || part.endsWith(".lock")) {
                    invalid = true;
                    break;
                }
            }
        }

        if (invalid) {
            throw new GitOperationException(GitErrorCode.INVALID_BRANCH);
        }
        return branch;
    }

    private Path getWorkspaceRoot() {
        Path configuredRoot = gitProperties.getWorkspaceRoot();
        if (configuredRoot == null) {
            throw new GitOperationException(GitErrorCode.WORKSPACE_PREPARATION_FAILED);
        }
        return configuredRoot.toAbsolutePath().normalize();
    }

    private String hashWorkspaceKey(Long githubId, RepositoryInfo repositoryInfo, String branch) {
        String source = githubId + "\u0000"
                + repositoryInfo.getOwner().toLowerCase(Locale.ROOT) + "\u0000"
                + repositoryInfo.getRepo().toLowerCase(Locale.ROOT) + "\u0000"
                + branch;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String normalizeRemoteUrl(String remoteUrl) {
        String normalized = remoteUrl == null ? "" : remoteUrl.trim();
        return normalized.endsWith(".git")
                ? normalized.substring(0, normalized.length() - 4)
                : normalized;
    }

    private String normalizeUsername(Long githubId, String username) {
        if (username == null || username.isBlank()) {
            return "github-user-" + githubId;
        }
        return username.trim();
    }

    private void deleteTemporaryClone(Path target, Path userRoot) {
        if (target == null
                || !target.normalize().startsWith(userRoot.normalize())
                || !target.getFileName().toString().startsWith(".clone-")
                || !Files.exists(target)) {
            return;
        }

        try (var paths = Files.walk(target)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 실패한 임시 clone은 다음 운영 정리 대상이므로 원래 예외를 보존한다.
                }
            });
        } catch (IOException ignored) {
            // 원래 clone/workspace 예외를 보존한다.
        }
    }

    private record WorkspaceRequest(
            Path userRoot,
            Path workspacePath,
            String cloneUrl,
            String branch,
            String accessToken,
            String username,
            String email
    ) {
        @Override
        public String toString() {
            return "WorkspaceRequest[workspacePath=%s, cloneUrl=%s, branch=%s, accessToken=***]"
                    .formatted(workspacePath, cloneUrl, branch);
        }
    }
}
