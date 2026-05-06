package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import org.springframework.stereotype.Service;

@Service
public class AccessibilityAnalysisService {

    private final AccessibilityAnalyzer accessibilityAnalyzer = new AccessibilityAnalyzer();

    public AccessibilityCheckResult analyzeSc111(String code) {
        return accessibilityAnalyzer.analyzeSc111(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc111Decorative(String code) {
        return accessibilityAnalyzer.analyzeSc111Decorative(code).get(0);
    }
}
