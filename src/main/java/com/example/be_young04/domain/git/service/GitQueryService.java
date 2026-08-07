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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitQueryService {

    private final GitCommandExecutor gitCommandExecutor;
    private final GitRepositoryValidator gitRepositoryValidator;

    public GitStatusResponse getStatus() {
        gitRepositoryValidator.validateRepository();

        GitCommandResult statusResult = gitCommandExecutor.execute(
                "-c", "core.quotepath=false", "status", "--porcelain=v1"
        );
        requireSuccess(statusResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        Map<String, GitFileStatus> statuses = parseStatuses(statusResult.stdout());
        Map<String, LineChanges> lineChanges = getLineChanges();

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

        return new GitStatusResponse(getCurrentBranch(), !files.isEmpty(), files);
    }

    public GitDiffResponse getDiff(String filePath) {
        gitRepositoryValidator.validateRepository();
        String validatedPath = gitRepositoryValidator.validateFilePath(filePath);

        GitCommandResult diffResult = gitCommandExecutor.execute(
                "-c", "core.quotepath=false", "diff", "HEAD", "--", validatedPath
        );
        requireSuccess(diffResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        String diff = diffResult.stdout();
        if (diff.isBlank() && !isTracked(validatedPath)) {
            GitCommandResult untrackedDiff = gitCommandExecutor.execute(
                    "-c", "core.quotepath=false", "diff", "--no-index", "--", "/dev/null", validatedPath
            );
            if (untrackedDiff.exitCode() != 0 && untrackedDiff.exitCode() != 1) {
                throw new GitOperationException(
                        GitErrorCode.COMMAND_EXECUTION_FAILED,
                        untrackedDiff.stderr()
                );
            }
            diff = untrackedDiff.stdout();
        }

        return new GitDiffResponse(validatedPath, diff);
    }

    public GitBranchResponse getBranches() {
        gitRepositoryValidator.validateRepository();

        GitCommandResult branchesResult = gitCommandExecutor.execute(
                "branch", "--format=%(refname:short)"
        );
        requireSuccess(branchesResult, GitErrorCode.COMMAND_EXECUTION_FAILED);

        List<String> branches = branchesResult.stdout().lines()
                .map(String::trim)
                .filter(branch -> !branch.isEmpty())
                .toList();

        return new GitBranchResponse(getCurrentBranch(), branches);
    }

    private String getCurrentBranch() {
        GitCommandResult branchResult = gitCommandExecutor.execute("branch", "--show-current");
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

    private Map<String, LineChanges> getLineChanges() {
        GitCommandResult numstatResult = gitCommandExecutor.execute(
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

    private boolean isTracked(String filePath) {
        return gitCommandExecutor.execute(
                "ls-files", "--error-unmatch", "--", filePath
        ).isSuccess();
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
