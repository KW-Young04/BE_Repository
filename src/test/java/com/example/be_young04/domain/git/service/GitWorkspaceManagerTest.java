package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.config.GitProperties;
import com.example.be_young04.domain.git.dto.request.GitCommitAndPushRequest;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.type.GitErrorCode;
import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
import com.example.be_young04.domain.repository.dto.RepositoryInfo;
import com.example.be_young04.domain.repository.service.GithubUrlParser;
import com.example.be_young04.domain.user.entity.GithubUser;
import com.example.be_young04.domain.user.service.GithubUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitWorkspaceManagerTest {

    @TempDir
    Path temporaryDirectory;

    private Path remotePath;
    private GitProperties gitProperties;
    private GitCommandExecutor gitCommandExecutor;
    private GitWorkspaceManager gitWorkspaceManager;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        remotePath = temporaryDirectory.resolve("remote.git");
        Path seedRepository = temporaryDirectory.resolve("seed");

        runGit(temporaryDirectory, "init", "--bare", "--initial-branch=main", remotePath.toString());
        Files.createDirectory(seedRepository);
        runGit(seedRepository, "init", "-b", "main");
        runGit(seedRepository, "config", "user.name", "CODEE Test");
        runGit(seedRepository, "config", "user.email", "codee-test@example.com");
        Files.writeString(seedRepository.resolve("README.md"), "workspace test\n");
        runGit(seedRepository, "add", "README.md");
        runGit(seedRepository, "commit", "-m", "initial commit");
        runGit(seedRepository, "remote", "add", "origin", remotePath.toUri().toString());
        runGit(seedRepository, "push", "origin", "main");

        gitProperties = new GitProperties();
        gitProperties.setWorkspaceRoot(temporaryDirectory.resolve("workspaces"));
        gitProperties.setCommandTimeoutSeconds(10);
        gitProperties.setDefaultRemote("origin");

        GithubUserService githubUserService = mock(GithubUserService.class);
        when(githubUserService.getById(anyLong())).thenAnswer(invocation -> GithubUser.builder()
                .githubId(invocation.getArgument(0))
                .username("test-user")
                .accessToken("secret-test-token")
                .build());

        GitRemoteUrlResolver remoteUrlResolver = mock(GitRemoteUrlResolver.class);
        when(remoteUrlResolver.resolve(any(RepositoryInfo.class)))
                .thenReturn(remotePath.toUri().toString());

        gitCommandExecutor = new GitCommandExecutor(gitProperties);
        gitWorkspaceManager = new GitWorkspaceManager(
                gitProperties,
                gitCommandExecutor,
                new GithubUrlParser(),
                githubUserService,
                remoteUrlResolver
        );
    }

    @Test
    void clonesOnceAndReusesTheSameUserRepositoryWorkspace() throws IOException {
        Path firstWorkspace = workspacePath(1L, "https://github.com/example/repository", "main");
        Files.writeString(firstWorkspace.resolve("local-change.txt"), "preserved\n");

        Path reusedWorkspace = workspacePath(1L, "https://github.com/example/repository", "main");

        assertThat(reusedWorkspace).isEqualTo(firstWorkspace);
        assertThat(reusedWorkspace.resolve(".git")).isDirectory();
        assertThat(reusedWorkspace.resolve("local-change.txt")).hasContent("preserved\n");
        assertThat(runGit(reusedWorkspace, "remote", "get-url", "origin"))
                .isEqualTo(remotePath.toUri().toString());
        assertThat(runGit(reusedWorkspace, "remote", "get-url", "origin"))
                .doesNotContain("secret-test-token");
        assertThat(runGit(reusedWorkspace, "config", "user.name")).isEqualTo("test-user");
        assertThat(runGit(reusedWorkspace, "config", "user.email"))
                .isEqualTo("1+test-user@users.noreply.github.com");
    }

    @Test
    void isolatesWorkspacesByUserAndRepository() {
        Path firstUser = workspacePath(1L, "https://github.com/example/repository", "main");
        Path secondUser = workspacePath(2L, "https://github.com/example/repository", "main");
        Path otherRepository = workspacePath(1L, "https://github.com/example/other", "main");

        assertThat(firstUser).isNotEqualTo(secondUser);
        assertThat(firstUser).isNotEqualTo(otherRepository);
        assertThat(secondUser).isNotEqualTo(otherRepository);
        assertThat(firstUser).isDirectory();
        assertThat(secondUser).isDirectory();
        assertThat(otherRepository).isDirectory();
    }

    @Test
    void rejectsInvalidBranchBeforeCreatingWorkspace() {
        assertThatThrownBy(() -> workspacePath(
                1L, "https://github.com/example/repository", "../main"
        ))
                .isInstanceOfSatisfying(GitOperationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(GitErrorCode.INVALID_BRANCH)
                );
    }

    @Test
    void commitsAndPushesChangesFromTheSelectedWorkspace() throws IOException {
        GitCommandService commandService = new GitCommandService(
                gitCommandExecutor,
                new GitRepositoryValidator(),
                gitProperties
        );

        String committedHash = gitWorkspaceManager.withWorkspace(
                1L,
                "https://github.com/example/repository",
                "main",
                context -> {
                    try {
                        Files.writeString(context.repositoryPath().resolve("feature.txt"), "feature\n");
                    } catch (IOException exception) {
                        throw new AssertionError(exception);
                    }

                    return commandService.commitAndPush(
                            context.repositoryPath(),
                            context.accessToken(),
                            new GitCommitAndPushRequest(
                                    "https://github.com/example/repository",
                                    "main",
                                    "feat: workspace push",
                                    List.of("feature.txt"),
                                    null
                            )
                    ).commit().commitHash();
                }
        );

        assertThat(runGit(
                temporaryDirectory,
                "--git-dir", remotePath.toString(), "rev-parse", "main"
        )).isEqualTo(committedHash);
    }

    private Path workspacePath(Long githubId, String repositoryUrl, String branch) {
        return gitWorkspaceManager.withWorkspace(
                githubId,
                repositoryUrl,
                branch,
                context -> context.repositoryPath()
        );
    }

    private String runGit(Path workingDirectory, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            assertThat(exitCode)
                    .withFailMessage("Git command failed: %s%n%s", String.join(" ", command), output)
                    .isZero();
            return output;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
