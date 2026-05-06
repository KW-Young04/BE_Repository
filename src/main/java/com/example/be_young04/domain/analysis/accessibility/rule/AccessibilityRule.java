package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;

public interface AccessibilityRule {
    AccessibilityCheckResult analyze(String code);
}
