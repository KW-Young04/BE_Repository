package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc412NameRoleValueRule implements AccessibilityRule {

    private static final Pattern INTERACTIVE_ELEMENT_PATTERN =
            Pattern.compile("<(?:button|a|input|select|textarea)\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern CUSTOM_INTERACTIVE_PATTERN =
            Pattern.compile("<(?:div|span|li|section)\\b(?=[^>]*(?:onClick\\s*=|role\\s*=))[^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern ROLE_PATTERN =
            Pattern.compile("\\brole\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)')", Pattern.CASE_INSENSITIVE);

    private static final Pattern ACCESSIBLE_NAME_PATTERN =
            Pattern.compile("\\b(?:aria-label|aria-labelledby|title)\\s*=", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATE_PATTERN =
            Pattern.compile("\\b(?:aria-checked|aria-expanded|aria-selected|aria-pressed|aria-disabled)\\s*=", Pattern.CASE_INSENSITIVE);

    private static final Pattern BUTTON_TEXT_PATTERN =
            Pattern.compile("<button\\b[^>]*>\\s*[^<\\s][^<]*</button>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        int customComponentCount = 0;

        if (code != null && !code.isBlank()) {
            inspectNativeControls(code, issues);

            Matcher matcher = CUSTOM_INTERACTIVE_PATTERN.matcher(code);
            while (matcher.find()) {
                customComponentCount++;
                String tag = matcher.group();

                boolean hasRole = ROLE_PATTERN.matcher(tag).find();
                boolean hasName = ACCESSIBLE_NAME_PATTERN.matcher(tag).find();
                boolean hasState = STATE_PATTERN.matcher(tag).find();

                if (!hasRole) {
                    issues.add(buildIssue(
                            "CUSTOM_CONTROL_ROLE_MISSING",
                            "커스텀 상호작용 요소에 role이 없습니다.",
                            code,
                            matcher.start(),
                            tag
                    ));
                }

                if (!hasName) {
                    issues.add(buildIssue(
                            "CUSTOM_CONTROL_NAME_MISSING",
                            "커스텀 상호작용 요소에 접근 가능한 이름이 없습니다.",
                            code,
                            matcher.start(),
                            tag
                    ));
                }

                if (hasRole && isStateRequiredRole(tag) && !hasState) {
                    issues.add(buildIssue(
                            "CUSTOM_CONTROL_STATE_MISSING",
                            "상태를 가져야 하는 커스텀 컴포넌트에 aria state 값이 없습니다.",
                            code,
                            matcher.start(),
                            tag
                    ));
                }
            }
        }

        boolean aiReviewRequired = issues.isEmpty() && customComponentCount > 0;

        return AccessibilityCheckResult.builder()
                .successCriteria("4.1.2")
                .name("이름·역할·값")
                .level("A")
                .mvpDescription("표준 HTML 컨트롤은 명세에 맞게 사용하고, 커스텀 컴포넌트는 접근 가능한 이름, 역할, 상태 값을 제공해야 합니다.")
                .implementationMethod("코드+AI")
                .implementationDescription("role, accessible name, aria state 존재 여부는 코드로 확인하고, 커스텀 컴포넌트의 의미 적절성은 AI로 검토합니다.")
                .status(issues.isEmpty() ? (aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS") : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "커스텀 컴포넌트의 role, name, state가 실제 UI 의미와 일치하는지 AI 검토가 필요합니다." : null)
                .issues(issues)
                .build();
    }

    private void inspectNativeControls(String code, List<AccessibilityIssue> issues) {
        Matcher matcher = INTERACTIVE_ELEMENT_PATTERN.matcher(code);

        while (matcher.find()) {
            String tag = matcher.group();

            if (tag.toLowerCase().startsWith("<button") && !ACCESSIBLE_NAME_PATTERN.matcher(tag).find()) {
                String buttonBlock = findButtonBlock(code, matcher.start());
                if (buttonBlock != null && !BUTTON_TEXT_PATTERN.matcher(buttonBlock).find()) {
                    issues.add(buildIssue(
                            "NATIVE_BUTTON_NAME_MISSING",
                            "button 요소에 접근 가능한 이름이 없습니다.",
                            code,
                            matcher.start(),
                            buttonBlock
                    ));
                }
            }

            if (tag.toLowerCase().startsWith("<a") && !tag.toLowerCase().contains("href")) {
                issues.add(buildIssue(
                        "LINK_HREF_MISSING",
                        "a 요소가 링크 역할을 하려면 href 속성이 필요합니다.",
                        code,
                        matcher.start(),
                        tag
                ));
            }
        }
    }

    private String findButtonBlock(String code, int startIndex) {
        int endIndex = code.indexOf("</button>", startIndex);
        if (endIndex == -1) {
            return null;
        }
        return code.substring(startIndex, Math.min(code.length(), endIndex + "</button>".length()));
    }

    private boolean isStateRequiredRole(String tag) {
        String lower = tag.toLowerCase();
        return lower.contains("role=\"checkbox\"")
                || lower.contains("role='checkbox'")
                || lower.contains("role=\"switch\"")
                || lower.contains("role='switch'")
                || lower.contains("role=\"tab\"")
                || lower.contains("role='tab'")
                || lower.contains("role=\"button\"")
                || lower.contains("role='button'");
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