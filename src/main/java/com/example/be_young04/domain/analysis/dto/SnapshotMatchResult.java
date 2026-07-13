package com.example.be_young04.domain.analysis.dto;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;

import java.util.List;

public record SnapshotMatchResult(
        List<MatchedTarget> matched,
        List<WcagCheckResult> fallback
) {
    public record MatchedTarget(WcagCheckResult result, List<String> snapshotIds) {
    }
}