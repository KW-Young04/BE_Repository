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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GitCommandExecutor {

    private final GitProperties gitProperties;

    public GitCommandResult execute(Path workingDirectory, String... arguments) {
        return executeInternal(workingDirectory, Map.of(), List.of(), arguments);
    }

    public GitCommandResult executeAuthenticated(
            Path workingDirectory,
            String accessToken,
            String... arguments
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GitOperationException(GitErrorCode.AUTHENTICATION_FAILED);
        }

        String credentials = Base64.getEncoder().encodeToString(
                ("x-access-token:" + accessToken).getBytes(StandardCharsets.UTF_8)
        );
        String authorizationHeader = "Authorization: Basic " + credentials;

        return executeInternal(
                workingDirectory,
                Map.of(
                        "GIT_TERMINAL_PROMPT", "0",
                        "GIT_CONFIG_COUNT", "1",
                        "GIT_CONFIG_KEY_0", "http.extraHeader",
                        "GIT_CONFIG_VALUE_0", authorizationHeader
                ),
                List.of(accessToken, credentials, authorizationHeader),
                arguments
        );
    }

    private GitCommandResult executeInternal(
            Path workingDirectory,
            Map<String, String> environment,
            List<String> sensitiveValues,
            String... arguments
    ) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toAbsolutePath().normalize().toFile());
        processBuilder.environment().putAll(environment);

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
                    redact(stdout, sensitiveValues),
                    redact(stderr, sensitiveValues)
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

    private String redact(String value, List<String> sensitiveValues) {
        String redacted = value;
        for (String sensitiveValue : sensitiveValues) {
            if (sensitiveValue != null && !sensitiveValue.isBlank()) {
                redacted = redacted.replace(sensitiveValue, "***");
            }
        }
        return redacted;
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
