package com.example.be_young04.domain.snapshot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SnapshotResponse {
    private String deploymentUrl;
    private String imagePath;
    private int width;
    private int height;
}