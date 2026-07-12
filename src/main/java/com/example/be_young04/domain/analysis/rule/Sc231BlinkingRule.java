package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 2.3.1 세 번 이하 점멸 - 비간섭 요건
 * 1초에 3회 초과 점멸 또는 일반/적색 점멸 임계값 초과 금지
 */
public class Sc231BlinkingRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findBlinkingIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.3.1")
                .name("세 번 이하 점멸 - 비간섭 요건")
                .level("A")
                .mvpDescription("1초에 3회 초과 점멸 또는 임계값을 초과하는 점멸을 하지 않습니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("CSS animation과 @keyframes에서 점멸 빈도를 분석합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("1초에 3회를 초과하는 점멸 애니메이션의 속도를 늦추거나 제거하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findBlinkingIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // @keyframes 내 blink 또는 animation 감지
        if (Pattern.compile("@keyframes\\s+[a-zA-Z0-9_-]*blink", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            // 빠른 애니메이션 (0.1초 이하) 감지
            if (Pattern.compile("animation:\\s*[^;]*\\s+(0\\.[0-1]|[0-9]m)s", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("EXCESSIVE_BLINKING")
                        .message("CSS 애니메이션에서 1초에 3회 이상의 점멸이 감지되었습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        // 빠른 setInterval 기반 애니메이션
        if (Pattern.compile("setInterval.*?100\\s*\\)|setInterval.*?50\\s*\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("RAPID_ANIMATION")
                    .message("JavaScript setInterval에서 빠른 애니메이션(100ms 이하)이 감지되었습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(48L);
    }
}
