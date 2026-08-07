package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.config.GitProperties;
import com.example.be_young04.domain.git.dto.request.GitCommitAndPushRequest;
import com.example.be_young04.domain.git.dto.request.GitCommitRequest;
import com.example.be_young04.domain.git.dto.request.GitPushRequest;
import com.example.be_young04.domain.git.dto.response.GitCommitAndPushResponse;
import com.example.be_young04.domain.git.dto.response.GitCommitResponse;
import com.example.be_young04.domain.git.dto.response.GitPushResponse;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.service.model.GitCommandResult;
import com.example.be_young04.domain.git.type.GitErrorCode;
import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GitCommandService {

    private static final Pattern REMOTE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._/-]*");

    private final GitCommandExecutor gitCommandExecutor;
    private final GitRepositoryValidator gitRepositoryValidator;
    private final GitProperties gitProperties;
    private final ReentrantLock repositoryLock = new ReentrantLock();

    public GitCommitResponse commit(GitCommitRequest request) {
        repositoryLock.lock();
        try {
            return commitInternal(request);
        } finally {
            repositoryLock.unlock();
        }
    }

    public GitPushResponse push(GitPushRequest request) {
        repositoryLock.lock();
        try {
            return pushInternal(request);
        } finally {
            repositoryLock.unlock();
        }
    }

    public GitCommitAndPushResponse commitAndPush(GitCommitAndPushRequest request) {
        repositoryLock.lock();
        try {
            GitCommitResponse commitResponse = commitInternal(
                    new GitCommitRequest(request.message(), request.files())
            );

            GitCommitAndPushResponse.GitOperationResult commitResult =
                    new GitCommitAndPushResponse.GitOperationResult(
                            true,
                            "GIT_COMMIT_SUCCESS",
                            commitResponse.message(),
                            commitResponse.commitHash()
                    );

            try {
                pushInternal(new GitPushRequest(request.remote()));
                return new GitCommitAndPushResponse(
                        commitResult,
                        new GitCommitAndPushResponse.GitOperationResult(
                                true,
                                "GIT_PUSH_SUCCESS",
                                "원격 저장소 push에 성공했습니다.",
                                null
                        )
                );
            } catch (GitOperationException e) {
                return new GitCommitAndPushResponse(
                        commitResult,
                        new GitCommitAndPushResponse.GitOperationResult(
                                false,
                                e.getErrorCode().getCode(),
                                e.getMessage(),
                                null
                        )
                );
            }
        } finally {
            repositoryLock.unlock();
        }
    }

    private GitCommitResponse commitInternal(GitCommitRequest request) {
        gitRepositoryValidator.validateRepository();
        ensureNoMergeConflicts();

        List<String> files = request.files().stream()
                .map(gitRepositoryValidator::validateFilePath)
                .distinct()
                .toList();

        List<String> addArguments = new ArrayList<>();
        addArguments.add("add");
        addArguments.add("--");
        addArguments.addAll(files);

        GitCommandResult addResult = gitCommandExecutor.execute(addArguments.toArray(String[]::new));
        requireSuccess(addResult, GitErrorCode.COMMIT_FAILED);

        GitCommandResult stagedChanges = gitCommandExecutor.execute("diff", "--cached", "--quiet", "--");
        if (stagedChanges.exitCode() == 0) {
            throw new GitOperationException(GitErrorCode.NO_CHANGES);
        }
        if (stagedChanges.exitCode() != 1) {
            throw new GitOperationException(GitErrorCode.COMMIT_FAILED, stagedChanges.stderr());
        }

        GitCommandResult commitResult = gitCommandExecutor.execute("commit", "-m", request.message());
        requireSuccess(commitResult, GitErrorCode.COMMIT_FAILED);

        GitCommandResult hashResult = gitCommandExecutor.execute("rev-parse", "HEAD");
        requireSuccess(hashResult, GitErrorCode.COMMIT_FAILED);

        int changedFileCount = getCommittedFileCount();
        return new GitCommitResponse(
                true,
                hashResult.stdout().trim(),
                request.message(),
                changedFileCount
        );
    }

    private GitPushResponse pushInternal(GitPushRequest request) {
        gitRepositoryValidator.validateRepository();

        String remote = normalizeRemote(request.remote());
        String branch = getCurrentBranch();
        GitCommandResult pushResult = gitCommandExecutor.execute("push", remote, branch);

        if (!pushResult.isSuccess()) {
            throw classifyPushFailure(pushResult);
        }

        return new GitPushResponse(true, remote, branch);
    }

    private void ensureNoMergeConflicts() {
        GitCommandResult conflicts = gitCommandExecutor.execute(
                "diff", "--name-only", "--diff-filter=U"
        );
        requireSuccess(conflicts, GitErrorCode.COMMIT_FAILED);

        if (!conflicts.stdout().isBlank()) {
            throw new GitOperationException(GitErrorCode.MERGE_CONFLICT);
        }
    }

    private int getCommittedFileCount() {
        GitCommandResult changedFiles = gitCommandExecutor.execute(
                "diff-tree", "--root", "--no-commit-id", "--name-only", "-r", "HEAD"
        );
        requireSuccess(changedFiles, GitErrorCode.COMMIT_FAILED);

        return (int) changedFiles.stdout().lines()
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .distinct()
                .count();
    }

    private String getCurrentBranch() {
        GitCommandResult branchResult = gitCommandExecutor.execute("branch", "--show-current");
        requireSuccess(branchResult, GitErrorCode.PUSH_FAILED);

        String branch = branchResult.stdout().trim();
        if (branch.isEmpty()) {
            throw new GitOperationException(
                    GitErrorCode.PUSH_FAILED,
                    "detached HEAD 상태에서는 push할 수 없습니다."
            );
        }
        return branch;
    }

    private String normalizeRemote(String requestedRemote) {
        String remote = requestedRemote == null || requestedRemote.isBlank()
                ? gitProperties.getDefaultRemote()
                : requestedRemote.trim();

        if (remote == null || !REMOTE_NAME_PATTERN.matcher(remote).matches()) {
            throw new GitOperationException(GitErrorCode.PUSH_FAILED, "올바르지 않은 remote 이름입니다.");
        }
        return remote;
    }

    private GitOperationException classifyPushFailure(GitCommandResult result) {
        String errorMessage = result.stderr().isBlank() ? result.stdout() : result.stderr();
        String normalized = errorMessage.toLowerCase(Locale.ROOT);

        if (normalized.contains("non-fast-forward")
                || normalized.contains("fetch first")
                || normalized.contains("updates were rejected")) {
            return new GitOperationException(GitErrorCode.NON_FAST_FORWARD, errorMessage);
        }

        if (normalized.contains("authentication failed")
                || normalized.contains("could not read username")
                || normalized.contains("permission denied")
                || normalized.contains("403")) {
            return new GitOperationException(GitErrorCode.AUTHENTICATION_FAILED, errorMessage);
        }

        return new GitOperationException(GitErrorCode.PUSH_FAILED, errorMessage);
    }

    private void requireSuccess(GitCommandResult result, GitErrorCode errorCode) {
        if (!result.isSuccess()) {
            throw new GitOperationException(errorCode, result.stderr());
        }
    }
}
