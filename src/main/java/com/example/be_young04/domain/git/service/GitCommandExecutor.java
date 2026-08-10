package com.example.be_young04.domain.git.service;

import com.example.be_young04.domain.git.config.GitProperties;
import com.example.be_young04.domain.git.exception.GitOperationException;
import com.example.be_young04.domain.git.service.model.GitCommandResult;
import com.example.be_young04.domain.git.type.GitErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GitCommandExecutor {

    private final GitProperties gitProperties;

    public GitCommandResult execute(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(
                gitProperties.getRepositoryPath().toFile()
        );

        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdoutFuture = readAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readAsync(process.getErrorStream());

            boolean finished = process.waitFor(
                    gitProperties.getCommandTimeoutSeconds(),
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                throw new GitOperationException(
                        GitErrorCode.COMMAND_TIMEOUT
                );
            }

            String stdout = await(stdoutFuture);
            String stderr = await(stderrFuture);

            return new GitCommandResult(
                    process.exitValue(),
                    stdout,
                    stderr
            );
        } catch (IOException e) {
            throw new GitOperationException(
                    GitErrorCode.COMMAND_EXECUTION_FAILED,
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new GitOperationException(
                    GitErrorCode.COMMAND_INTERRUPTED,
                e
            );
        }
    }

    private CompletableFuture<String> readAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private String await(CompletableFuture<String> outputFuture) {
        try {
            return outputFuture.join();
        } catch (CompletionException e) {
            throw new GitOperationException(GitErrorCode.COMMAND_EXECUTION_FAILED, e.getCause());
        }
    }
}
