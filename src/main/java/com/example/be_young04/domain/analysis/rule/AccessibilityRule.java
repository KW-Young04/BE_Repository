package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;

import java.util.List;

public interface AccessibilityRule {
    AccessibilityCheckResult analyze(String code);

    // 이 룰이 담당하는 WCAG_ITEMS 고정 PK 목록 (대부분 원소 1개, 2.2.1처럼 예외적으로 여러 개)
    List<Long> getWcagItemIds();

    // 루트 문서 파일(index.html, App.tsx 등)에서만 실행되어야 하는 룰인지 여부
    default boolean isRootDocumentOnly() {
        return false;
    }
}