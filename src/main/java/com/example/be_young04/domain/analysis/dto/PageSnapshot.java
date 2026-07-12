package com.example.be_young04.domain.analysis.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record PageSnapshot(
        String snapshotId,
        MultipartFile image,
        List<String> renderedFilePaths
) {
}