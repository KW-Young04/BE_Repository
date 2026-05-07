package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc243FocusOrderRule implements AccessibilityRule {

    private static final Pattern TABINDEX_PATTERN =
            Pattern.compile("\\btabindex\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|\\{?(-?\\d+)\\}?)", Pattern.CASE_INSENSITIVE);

    private static final Pattern FOCUSABLE_ELEMENT_PATTERN =
            Pattern.compile("<(?:a|button|input|select|textarea)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        boolean hasFocusableElement = false;
        boolean hasTabindex = false;
        boolean hasPositiveTabindex = false;

        if (code != null && !code.isBlank()) {
            hasFocusableElement = FOCUSABLE_ELEMENT_PATTERN.matcher(code).find();

            Matcher matcher = TABINDEX_PATTERN.matcher(code);
            while (matcher.find()) {
                hasTabindex = true;

                String rawValue = getMatchedValue(matcher);
                Integer tabindex = parseInteger(rawValue);

                if (tabindex != null && tabindex > 0) {
                    hasPositiveTabindex = true;
                    issues.add(buildIssue(
                            "POSITIVE_TABINDEX_DETECTED",
                            "양수 tabindex 값은 자연스러운 포커스 순서를 왜곡할 수 있습니다.",
                            code,
                            matcher.start(),
                            matcher.group()
                    ));
                }
            }
        }

        boolean aiReviewRequired = hasFocusableElement || hasTabindex;

        return AccessibilityCheckResult.builder()
                .successCriteria("2.4.3")
                .name("포커스 순서")
                .level("A")
                .mvpDescription("키보드 탐색 시 의미와 조작에 영향을 주는 포커스 순서가 논리적이어야 합니다.")
                .implementationMethod("코드+AI")
                .implementationDescription("tabindex 값은 코드로 분석하고, 실제 DOM 구조와 사용자 흐름의 논리성은 AI 판단이 필요합니다.")
                .status(issues.isEmpty() ? (aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS") : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "DOM 구조와 실제 사용자 흐름 기준으로 포커스 이동 순서가 논리적인지 검토해야 합니다." : null)
                .issues(issues)
                .build();
    }

    private String getMatchedValue(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) {
                return matcher.group(i);
            }
        }
        return null;
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AccessibilityIssue buildIssue(String type, String message, String code, int startIndex, String snippet) {
        return AccessibilityIssue.builder()
                .type(type)
                .message(message)
                .line(calculateLineNumber(code, startIndex))
                .snippet(normalizeSnippet(snippet))
                .build();
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