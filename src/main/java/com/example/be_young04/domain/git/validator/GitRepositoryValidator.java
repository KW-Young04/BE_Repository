package com.example.be_young04.domain.git.validator;

import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.type.GitErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
public class GitRepositoryValidator {

    public void validateRepository(Path requestedRepositoryPath) {
        Path repositoryPath = normalizeRepositoryPath(requestedRepositoryPath);

        if (!Files.isDirectory(repositoryPath) || !Files.exists(repositoryPath.resolve(".git"))) {
            throw new GitOperationException(GitErrorCode.NOT_GIT_REPOSITORY);
        }
    }

    public String validateFilePath(Path requestedRepositoryPath, String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new GitOperationException(GitErrorCode.INVALID_FILE_PATH);
        }

        Path repositoryPath = normalizeRepositoryPath(requestedRepositoryPath);

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

            Path existingAncestor = resolvedPath;
            while (existingAncestor != null && !Files.exists(existingAncestor)) {
                existingAncestor = existingAncestor.getParent();
            }

            if (existingAncestor == null
                    || !existingAncestor.toRealPath().startsWith(repositoryPath.toRealPath())) {
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

    private Path normalizeRepositoryPath(Path repositoryPath) {
        if (repositoryPath == null) {
            throw new GitOperationException(GitErrorCode.NOT_GIT_REPOSITORY);
        }

        return repositoryPath.toAbsolutePath().normalize();
    }
}
