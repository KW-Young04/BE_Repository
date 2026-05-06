package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc231FlashThresholdRule implements AccessibilityRule {

    private static final Pattern KEYFRAMES_PATTERN =
            Pattern.compile("@keyframes\\s+[a-zA-Z0-9_-]+\\s*\\{", Pattern.CASE_INSENSITIVE);

    private static final Pattern ANIMATION_PROPERTY_PATTERN =
            Pattern.compile("\\banimation(?:-name|-duration|-iteration-count)?\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern BLINK_PATTERN =
            Pattern.compile("\\bblink\\b|visibility\\s*:\\s*hidden|opacity\\s*:\\s*0", Pattern.CASE_INSENSITIVE);

    private static final Pattern RED_COLOR_PATTERN =
            Pattern.compile("(?:color|background|background-color)\\s*:\\s*(?:red|#f00|#ff0000|rgb\\s*\\(\\s*255\\s*,\\s*0\\s*,\\s*0\\s*\\))",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern FAST_DURATION_PATTERN =
            Pattern.compile("animation(?:-duration)?\\s*:\\s*(?:0?\\.\\d+s|[1-9]\\d*ms)", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        boolean hasAnimation = false;
        boolean hasBlinkPattern = false;
        boolean hasRedFlashRisk = false;
        boolean hasFastDuration = false;

        if (code != null && !code.isBlank()) {
            hasAnimation = inspectPattern(code, issues, KEYFRAMES_PATTERN,
                    "FLASH_KEYFRAMES_DETECTED",
                    "@keyframes 기반 애니메이션이 감지되었습니다. 점멸 빈도 검토가 필요합니다.");

            hasAnimation |= inspectPattern(code, issues, ANIMATION_PROPERTY_PATTERN,
                    "FLASH_ANIMATION_DETECTED",
                    "CSS animation 속성이 감지되었습니다. 점멸 효과 여부 검토가 필요합니다.");

            hasBlinkPattern = inspectPattern(code, issues, BLINK_PATTERN,
                    "FLASH_BLINK_PATTERN_DETECTED",
                    "깜빡임 또는 점멸로 이어질 수 있는 opacity/visibility 패턴이 감지되었습니다.");

            hasRedFlashRisk = inspectPattern(code, issues, RED_COLOR_PATTERN,
                    "RED_FLASH_RISK_DETECTED",
                    "적색 점멸 가능성이 있는 색상 사용이 감지되었습니다.");

            hasFastDuration = inspectPattern(code, issues, FAST_DURATION_PATTERN,
                    "FAST_FLASH_DURATION_DETECTED",
                    "짧은 animation duration이 감지되었습니다. 1초에 3회 초과 점멸 가능성을 검토해야 합니다.");
        }

        boolean aiReviewRequired = hasAnimation || hasBlinkPattern || hasRedFlashRisk || hasFastDuration;

        return AccessibilityCheckResult.builder()
                .successCriteria("2.3.1")
                .name("세 번 이하 점멸 - 비간섭 조건")
                .level("A")
                .mvpDescription("1초에 3회 초과 점멸 또는 일반/적색 점멸 임계값 초과를 금지해야 합니다.")
                .implementationMethod("코드+AI")
                .implementationDescription("CSS animation, @keyframes, opacity, visibility, red color 패턴은 코드로 탐지하고 실제 점멸 빈도와 위험성은 AI 판단이 필요합니다.")
                .status(aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "감지된 애니메이션이 실제로 1초에 3회 초과 점멸하거나 적색 점멸 임계값을 초과하는지 검토해야 합니다." : null)
                .issues(issues)
                .build();
    }

    private boolean inspectPattern(String code, List<AccessibilityIssue> issues, Pattern pattern, String type, String message) {
        boolean detected = false;
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            detected = true;
            issues.add(buildIssue(type, message, code, matcher.start(), extractSnippet(code, matcher.start(), matcher.end())));
        }

        return detected;
    }

    private AccessibilityIssue buildIssue(String type, String message, String code, int startIndex, String snippet) {
        return AccessibilityIssue.builder()
                .type(type)
                .message(message)
                .line(calculateLineNumber(code, startIndex))
                .snippet(normalizeSnippet(snippet))
                .build();
    }

    private String extractSnippet(String code, int start, int end) {
        int snippetStart = Math.max(0, start - 80);
        int snippetEnd = Math.min(code.length(), end + 120);
        return code.substring(snippetStart, snippetEnd);
    }

    private int calculateLineNumber(String code, int startIndex) {
        int line = 1;
        for (int i = 0; i < startIndex && i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String normalizeSnippet(String snippet) {
        String normalized = snippet.replaceAll("\\s+", " ").trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
    }
}