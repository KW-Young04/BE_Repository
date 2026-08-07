package com.example.be_young04.domain.git.dto.response;

import com.example.be_young04.domain.git.type.GitFileStatus;

public record GitFileChangeResponse(
        String path,
        GitFileStatus status,
        int addedLines,
        int deletedLines
) {
}