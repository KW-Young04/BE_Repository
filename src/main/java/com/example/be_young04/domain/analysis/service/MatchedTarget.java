package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;

import java.util.List;

/**
 * 판정 대상 결과 하나와, 그 결과의 filePath를 렌더링한 스냅샷 id 목록(1:N)의 짝.
 */
record MatchedTarget(WcagCheckResult result, List<String> snapshotIds) {
}