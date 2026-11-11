package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.config.GitProperties;
import com.example.be_young04.domain.git.dto.request.GitCommitAndPushRequest;
import com.example.be_young04.domain.git.dto.request.GitCommitRequest;
import com.example.be_young04.domain.git.dto.request.GitFileWriteRequest;
import com.example.be_young04.domain.git.dto.response.GitCommitAndPushResponse;
import com.example.be_young04.domain.git.dto.response.GitCommitResponse;
import com.example.be_young04.domain.git.dto.response.GitDiffResponse;
import com.example.be_young04.domain.git.dto.response.GitStatusResponse;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.type.GitErrorCode;
import com.example.be_young04.domain.git.type.GitFileStatus;
import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
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
import static org.assertj.core.groups.Tuple.tuple;

class GitServiceTest {

    @TempDir
    Path temporaryDirectory;

    private Path repositoryPath;
    private GitQueryService gitQueryService;
    private GitCommandService gitCommandService;
    private GitWorkingTreeService gitWorkingTreeService;
    private GitRepositoryValidator gitRepositoryValidator;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        repositoryPath = Files.createDirectory(temporaryDirectory.resolve("repository"));
        runGit(repositoryPath, "init", "-b", "main");
        runGit(repositoryPath, "config", "user.name", "CODEE Test");
        runGit(repositoryPath, "config", "user.email", "codee-test@example.com");

        Files.writeString(repositoryPath.resolve("README.md"), "first line\n");
        runGit(repositoryPath, "add", "README.md");
        runGit(repositoryPath, "commit", "-m", "initial commit");

        GitProperties gitProperties = new GitProperties();
        gitProperties.setWorkspaceRoot(temporaryDirectory.resolve("workspaces"));
        gitProperties.setCommandTimeoutSeconds(10);
        gitProperties.setDefaultRemote("origin");

        GitCommandExecutor gitCommandExecutor = new GitCommandExecutor(gitProperties);
        gitRepositoryValidator = new GitRepositoryValidator();
        gitQueryService = new GitQueryService(gitCommandExecutor, gitRepositoryValidator);
        gitWorkingTreeService = new GitWorkingTreeService(gitRepositoryValidator);
        gitCommandService = new GitCommandService(
                gitCommandExecutor,
                gitRepositoryValidator,
                gitProperties
        );
    }

    @Test
    void statusDiffAndBranchesReturnWorkingTreeChanges() throws IOException {
        Files.writeString(
                repositoryPath.resolve("README.md"),
                "second line\n",
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND
        );
        Files.writeString(repositoryPath.resolve("new-file.txt"), "new content\n");

        GitStatusResponse status = gitQueryService.getStatus(repositoryPath);
        GitDiffResponse trackedDiff = gitQueryService.getDiff(repositoryPath, "README.md");
        GitDiffResponse untrackedDiff = gitQueryService.getDiff(repositoryPath, "new-file.txt");

        assertThat(status.branch()).isEqualTo("main");
        assertThat(status.hasChanges()).isTrue();
        assertThat(status.files())
                .extracting(file -> file.path(), file -> file.status())
                .containsExactlyInAnyOrder(
                        tuple("README.md", GitFileStatus.MODIFIED),
                        tuple("new-file.txt", GitFileStatus.UNTRACKED)
                );
        assertThat(trackedDiff.diff()).contains("+second line");
        assertThat(untrackedDiff.diff()).contains("+new content");
        assertThat(gitQueryService.getBranches(repositoryPath).branches()).containsExactly("main");
    }

    @Test
    void commitAndPushWritesCommitToLocalBareRemote() throws IOException, InterruptedException {
        Path remotePath = temporaryDirectory.resolve("remote.git");
        runGit(temporaryDirectory, "init", "--bare", remotePath.toString());
        runGit(repositoryPath, "remote", "add", "origin", remotePath.toString());
        runGit(repositoryPath, "push", "-u", "origin", "main");

        Files.writeString(repositoryPath.resolve("feature.txt"), "feature content\n");

        GitCommitAndPushResponse response = gitCommandService.commitAndPush(
                repositoryPath,
                "test-token",
                new GitCommitAndPushRequest(
                        "https://github.com/example/repository",
                        "main",
                        "feat: add feature file",
                        List.of("feature.txt"),
                        null
                )
        );

        String localHead = runGit(repositoryPath, "rev-parse", "HEAD");
        String remoteHead = runGit(temporaryDirectory, "--git-dir", remotePath.toString(), "rev-parse", "main");

        assertThat(response.commit().success()).isTrue();
        assertThat(response.push().success()).isTrue();
        assertThat(response.commit().commitHash()).isEqualTo(localHead);
        assertThat(remoteHead).isEqualTo(localHead);
    }

    @Test
    void commitRejectsRequestWhenSelectedFileHasNoChanges() {
        assertThatThrownBy(() -> gitCommandService.commit(
                repositoryPath,
                new GitCommitRequest(
                        "https://github.com/example/repository",
                        "main",
                        "test: no changes",
                        List.of("README.md")
                )
        ))
                .isInstanceOfSatisfying(GitOperationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(GitErrorCode.NO_CHANGES)
                );
    }

    @Test
    void validatorRejectsPathOutsideRepository() {
        assertThatThrownBy(() -> gitRepositoryValidator.validateFilePath(repositoryPath, "../outside.txt"))
                .isInstanceOfSatisfying(GitOperationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(GitErrorCode.INVALID_FILE_PATH)
                );
    }

    @Test
    void writesEditedFileIntoSelectedWorkingTree() {
        gitWorkingTreeService.writeFile(
                repositoryPath,
                new GitFileWriteRequest(
                        "https://github.com/example/repository",
                        "main",
                        "src/new-file.txt",
                        "edited content\n"
                )
        );

        assertThat(repositoryPath.resolve("src/new-file.txt")).hasContent("edited content\n");
        assertThat(gitQueryService.getStatus(repositoryPath).files())
                .extracting(file -> file.path(), file -> file.status())
                .contains(tuple("src/new-file.txt", GitFileStatus.UNTRACKED));
    }

    @Test
    void validatorRejectsNewFileBelowSymlinkOutsideRepository() throws IOException {
        Path outsideDirectory = Files.createDirectory(temporaryDirectory.resolve("outside"));
        Files.createSymbolicLink(repositoryPath.resolve("outside-link"), outsideDirectory);

        assertThatThrownBy(() -> gitRepositoryValidator.validateFilePath(
                repositoryPath, "outside-link/new-file.txt"
        ))
                .isInstanceOfSatisfying(GitOperationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(GitErrorCode.INVALID_FILE_PATH)
                );
    }

    private String runGit(Path workingDirectory, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

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
    }
}
