package com.example.be_young04.domain.analysis.dto;

import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class CodeFixDto {

    private CodeFixDto() {
    }

    public record GenerateRequest(
            @NotBlank String repositoryUrl,
            @NotBlank String branchName,
            @NotBlank String filePath
    ) {
    }

    public record ApplyRequest(
            @NotBlank String repositoryUrl,
            @NotBlank String branchName,
            @NotBlank String filePath,
            @NotEmpty List<Long> locationIds
    ) {
    }

    public record FixItem(
            Long locationId,
            String sc,
            String status,      // READY / FAILED
            String before,
            String after,
            String failReason
    ) {
        public static FixItem ready(Long locationId, String sc, String before, String after) {
            return new FixItem(locationId, sc, "READY", before, after, null);
        }

        public static FixItem failed(Long locationId, String sc, String failReason) {
            return new FixItem(locationId, sc, "FAILED", null, null, failReason);
        }
    }

    public record GenerateResponse(String filePath, List<FixItem> fixes) {
    }

    public record ApplyResponse(
            String filePath,
            List<Long> appliedLocationIds,
            List<FixItem> failed,
            RealtimeAnalysisResponse remainingIssues
    ) {
    }
}