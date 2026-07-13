package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;

import java.util.List;

/**
 * 스냅샷 매칭 결과.
 * matched: 이미지와 매칭된 (결과, 스냅샷id목록) 쌍의 리스트
 * fallback: 매칭되는 스냅샷이 없어 이미지 없이 텍스트만으로 판단해야 하는 결과 목록
 */
record SnapshotMatchResult(
        List<MatchedTarget> matched,
        List<WcagCheckResult> fallback
) {
}