package com.example.be_young04.domain.snapshot.dto;

import java.util.List;

public record SnapshotMetaDto(
        String snapshotId,
        List<String> renderedFilePaths
) {
}