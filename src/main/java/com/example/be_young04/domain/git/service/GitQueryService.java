package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.dto.response.GitBranchResponse;
import com.example.be_young04.domain.git.dto.response.GitDiffResponse;
import com.example.be_young04.domain.git.dto.response.GitFileChangeResponse;
import com.example.be_young04.domain.git.dto.response.GitStatusResponse;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.service.model.GitCommandResult;
import com.example.be_young04.domain.git.type.GitErrorCode;
import com.example.be_young04.domain.git.type.GitFileStatus;
import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitQueryService {
    private final GitCommandExecutor gitCommandExecutor;
    private final GitRepositoryValidator gitRepositoryValidator;

    public GitStatusResponse getStatus(Path repositoryPath) {
        gitRepositoryValidator.validateRepository(repositoryPath);

        GitCommandResult statusResult = gitCommandExecutor.execute(
                repositoryPath,
                "-c", "core.quotepath=false", "status", "--porcelain=v1", "--untracked-files=all"
        );
        requireSuccess(statusResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        Map<String, GitFileStatus> statuses = parseStatuses(statusResult.stdout());
        Map<String, LineChanges> lineChanges = getLineChanges(repositoryPath);

        List<GitFileChangeResponse> files = statuses.entrySet().stream()
                .map(entry -> {
                    LineChanges changes = lineChanges.getOrDefault(entry.getKey(), LineChanges.EMPTY);
                    return new GitFileChangeResponse(
                            entry.getKey(),
                            entry.getValue(),
                            changes.addedLines(),
                            changes.deletedLines()
                    );
                })
                .toList();

        return new GitStatusResponse(getCurrentBranch(repositoryPath), !files.isEmpty(), files);
    }

    public GitDiffResponse getDiff(Path repositoryPath, String filePath) {
        gitRepositoryValidator.validateRepository(repositoryPath);
        String validatedPath = gitRepositoryValidator.validateFilePath(repositoryPath, filePath);

        GitCommandResult diffResult = gitCommandExecutor.execute(
                repositoryPath,
                "-c", "core.quotepath=false", "diff", "HEAD", "--", validatedPath
        );
        requireSuccess(diffResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        String diff = diffResult.stdout();
        if (diff.isBlank() && !isTracked(repositoryPath, validatedPath)) {
            diff = getUntrackedFileDiff(repositoryPath, validatedPath);
        }

        return new GitDiffResponse(validatedPath, diff);
    }

    public GitBranchResponse getBranches(Path repositoryPath) {
        gitRepositoryValidator.validateRepository(repositoryPath);

        GitCommandResult branchesResult = gitCommandExecutor.execute(
                repositoryPath,
                "branch", "--format=%(refname:short)"
        );
        requireSuccess(branchesResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        List<String> branches = branchesResult.stdout().lines()
                .map(String::trim)
                .filter(branch -> !branch.isEmpty())
                .toList();

        return new GitBranchResponse(getCurrentBranch(repositoryPath), branches);
    }

    private String getCurrentBranch(Path repositoryPath) {
        GitCommandResult branchResult = gitCommandExecutor.execute(
                repositoryPath, "branch", "--show-current"
        );
        requireSuccess(branchResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        String branch = branchResult.stdout().trim();
        return branch.isEmpty() ? "(detached)" : branch;
    }

    private Map<String, GitFileStatus> parseStatuses(String output) {
        Map<String, GitFileStatus> statuses = new LinkedHashMap<>();

        for (String line : output.lines().toList()) {
            if (line.length() < 4) {
                continue;
            }

            String code = line.substring(0, 2);
            String path = line.substring(3);
            if ((code.contains("R") || code.contains("C")) && path.contains(" -> ")) {
                path = path.substring(path.indexOf(" -> ") + 4);
            }

            statuses.put(path, toFileStatus(code));
        }

        return statuses;
    }

    private GitFileStatus toFileStatus(String code) {
        if ("??".equals(code)) {
            return GitFileStatus.UNTRACKED;
        }
        if (code.contains("R") || code.contains("C")) {
            return GitFileStatus.RENAMED;
        }
        if (code.contains("D")) {
            return GitFileStatus.DELETED;
        }
        if (code.contains("A")) {
            return GitFileStatus.ADDED;
        }
        return GitFileStatus.MODIFIED;
    }

    private Map<String, LineChanges> getLineChanges(Path repositoryPath) {
        GitCommandResult numstatResult = gitCommandExecutor.execute(
                repositoryPath,
                "-c", "core.quotepath=false", "diff", "--numstat", "HEAD", "--"
        );
        if (!numstatResult.isSuccess()) {
            return Map.of();
        }

        Map<String, LineChanges> changes = new LinkedHashMap<>();
        for (String line : numstatResult.stdout().lines().toList()) {
            String[] parts = line.split("\\t", 3);
            if (parts.length != 3) {
                continue;
            }

            changes.put(
                    parts[2],
                    new LineChanges(parseLineCount(parts[0]), parseLineCount(parts[1]))
            );
        }
        return changes;
    }

    private int parseLineCount(String value) {
        if ("-".equals(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isTracked(Path repositoryPath, String filePath) {
        return gitCommandExecutor.execute(
                repositoryPath,
                "ls-files", "--error-unmatch", "--", filePath
        ).isSuccess();
    }

    private String getUntrackedFileDiff(Path repositoryPath, String filePath) {
        Path emptyFile = null;
        try {
            emptyFile = Files.createTempFile("git-empty-", ".tmp");
            GitCommandResult untrackedDiff = gitCommandExecutor.execute(
                    repositoryPath,
                    "-c", "core.quotepath=false", "diff", "--no-index", "--",
                    emptyFile.toString(), filePath
            );
            if (untrackedDiff.exitCode() != 0 && untrackedDiff.exitCode() != 1) {
                throw new GitOperationException(
                        GitErrorCode.COMMAND_EXECUTION_FAILED,
                        untrackedDiff.stderr()
                );
            }
            return untrackedDiff.stdout();
        } catch (IOException exception) {
            throw new GitOperationException(GitErrorCode.COMMAND_EXECUTION_FAILED, exception);
        } finally {
            if (emptyFile != null) {
                try {
                    Files.deleteIfExists(emptyFile);
                } catch (IOException ignored) {
                    // 임시 diff 기준 파일 삭제 실패는 원래 Git 결과에 영향을 주지 않는다.
                }
            }
        }
    }

    private void requireSuccess(GitCommandResult result, GitErrorCode errorCode) {
        if (!result.isSuccess()) {
            throw new GitOperationException(errorCode, result.stderr());
        }
    }

    private record LineChanges(int addedLines, int deletedLines) {
        private static final LineChanges EMPTY = new LineChanges(0, 0);
    }
}
