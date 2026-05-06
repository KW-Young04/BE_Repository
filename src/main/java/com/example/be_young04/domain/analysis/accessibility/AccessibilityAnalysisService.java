package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessibilityAnalysisService {

    private final AccessibilityAnalyzer accessibilityAnalyzer = new AccessibilityAnalyzer();
    private final AccessibilityAiReviewService accessibilityAiReviewService;

    public AccessibilityCheckResult analyzeSc111(String code) {
        return accessibilityAnalyzer.analyzeSc111(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc111Decorative(String code) {
        return accessibilityAnalyzer.analyzeSc111Decorative(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc111ControlInput(String code) {
        AccessibilityCheckResult staticResult = accessibilityAnalyzer.analyzeSc111ControlInput(code).get(0);
        return accessibilityAiReviewService.reviewControlInputName(code, staticResult);
    }

    public AccessibilityCheckResult analyzeSc121(String code) {
        return accessibilityAnalyzer.analyzeSc121(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc121VideoOnly(String code) {
        return accessibilityAnalyzer.analyzeSc121VideoOnly(code).get(0);
    }
}
