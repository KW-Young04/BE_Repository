package com.example.be_young04.domain.repository.exception;

public class InvalidGithubUrlException extends RuntimeException {
    public InvalidGithubUrlException(String message) {
        super(message);
    }
}