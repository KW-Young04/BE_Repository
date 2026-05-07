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

    public AccessibilityCheckResult analyzeSc231(String code) {
        return accessibilityAnalyzer.analyzeSc231(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc241(String code) {
        return accessibilityAnalyzer.analyzeSc241(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc242(String code) {
        return accessibilityAnalyzer.analyzeSc242(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc243(String code) {
        return accessibilityAnalyzer.analyzeSc243(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc311(String code) {
        return accessibilityAnalyzer.analyzeSc311(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc321(String code) {
        return accessibilityAnalyzer.analyzeSc321(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc322(String code) {
        return accessibilityAnalyzer.analyzeSc322(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc331(String code) {
        return accessibilityAnalyzer.analyzeSc331(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc332(String code) {
        return accessibilityAnalyzer.analyzeSc332(code).get(0);
    }

    public AccessibilityCheckResult analyzeSc412(String code) {
        return accessibilityAnalyzer.analyzeSc412(code).get(0);
    }
}