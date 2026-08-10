package com.example.be_young04.domain.git.validator;

import com.example.be_young04.domain.git.config.GitProperties;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.type.GitErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class GitRepositoryValidator {

    private final GitProperties gitProperties;

    public void validateRepository() {
        Path repositoryPath = getRepositoryPath();

        if (!Files.isDirectory(repositoryPath) || !Files.exists(repositoryPath.resolve(".git"))) {
            throw new GitOperationException(GitErrorCode.NOT_GIT_REPOSITORY);
        }
    }

    public String validateFilePath(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new GitOperationException(GitErrorCode.INVALID_FILE_PATH);
        }

        Path repositoryPath = getRepositoryPath();

        try {
            Path relativePath = Path.of(requestedPath);
            if (relativePath.isAbsolute()) {
                throw new GitOperationException(GitErrorCode.INVALID_FILE_PATH);
            }

            Path resolvedPath = repositoryPath.resolve(relativePath).normalize();
            Path gitDirectory = repositoryPath.resolve(".git").normalize();

            if (!resolvedPath.startsWith(repositoryPath)
                    || resolvedPath.equals(repositoryPath)
                    || resolvedPath.startsWith(gitDirectory)) {
                throw new GitOperationException(GitErrorCode.INVALID_FILE_PATH);
            }

            if (Files.exists(resolvedPath)
                    && !resolvedPath.toRealPath().startsWith(repositoryPath.toRealPath())) {
                throw new GitOperationException(GitErrorCode.INVALID_FILE_PATH);
            }

            return repositoryPath
                    .relativize(resolvedPath)
                    .toString()
                    .replace("\\", "/");
        } catch (InvalidPathException | IOException e) {
            throw new GitOperationException(GitErrorCode.INVALID_FILE_PATH, e);
        }
    }

    private Path getRepositoryPath() {
        Path repositoryPath = gitProperties.getRepositoryPath();
        if (repositoryPath == null) {
            throw new GitOperationException(GitErrorCode.NOT_GIT_REPOSITORY);
        }

        return repositoryPath.toAbsolutePath().normalize();
    }
}
