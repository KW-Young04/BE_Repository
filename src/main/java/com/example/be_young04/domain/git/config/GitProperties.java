package com.example.be_young04.domain.git.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.git")
public class GitProperties {

    private Path repositoryPath;
    private int commandTimeoutSeconds = 30;
    private String defaultRemote = "origin";
}