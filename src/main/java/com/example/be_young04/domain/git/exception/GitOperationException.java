package com.example.be_young04.domain.git.exception;

import com.example.be_young04.domain.git.type.GitErrorCode;
import lombok.Getter;

@Getter
public class GitOperationException extends RuntimeException {

    private final GitErrorCode errorCode;
    private final String gitErrorMessage;

    public GitOperationException(GitErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.gitErrorMessage = null;
    }

    public GitOperationException(
            GitErrorCode errorCode,
            String gitErrorMessage
    ) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.gitErrorMessage = gitErrorMessage;
    }

    public GitOperationException(
            GitErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.gitErrorMessage = null;
    }
}