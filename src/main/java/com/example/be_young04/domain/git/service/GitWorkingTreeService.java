package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.dto.request.GitFileWriteRequest;
import com.example.be_young04.domain.git.dto.response.GitFileWriteResponse;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.type.GitErrorCode;
import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class GitWorkingTreeService {

    private final GitRepositoryValidator gitRepositoryValidator;

    public GitFileWriteResponse writeFile(Path repositoryPath, GitFileWriteRequest request) {
        gitRepositoryValidator.validateRepository(repositoryPath);
        String relativePath = gitRepositoryValidator.validateFilePath(repositoryPath, request.path());
        Path target = repositoryPath.resolve(relativePath).normalize();

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, request.content(), StandardCharsets.UTF_8);
            return new GitFileWriteResponse(true, relativePath);
        } catch (IOException exception) {
            throw new GitOperationException(GitErrorCode.COMMAND_EXECUTION_FAILED, exception);
        }
    }
}
