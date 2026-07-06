package com.example.be_young04.domain.snapshot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SnapshotResponse {
    private byte[] imageBytes;
    private int width;
    private int height;
}