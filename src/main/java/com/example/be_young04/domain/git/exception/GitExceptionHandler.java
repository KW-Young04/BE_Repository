package com.example.be_young04.domain.git.exception;

import com.example.be_young04.domain.git.controller.GitController;
import com.example.be_young04.domain.git.dto.response.GitErrorResponse;
import com.example.be_young04.domain.git.type.GitErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = GitController.class)
public class GitExceptionHandler {

    @ExceptionHandler(GitOperationException.class)
    public ResponseEntity<GitErrorResponse> handleGitOperationException(GitOperationException exception) {
        GitErrorCode errorCode = exception.getErrorCode();
        String message = errorCode.getMessage();
        if (exception.getGitErrorMessage() != null && !exception.getGitErrorMessage().isBlank()) {
            message = message + ": " + exception.getGitErrorMessage().trim();
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new GitErrorResponse(false, errorCode.getCode(), message));
    }
}
