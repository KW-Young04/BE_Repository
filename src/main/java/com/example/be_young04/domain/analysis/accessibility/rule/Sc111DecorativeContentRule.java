package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc111DecorativeContentRule implements AccessibilityRule {

    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALT_ATTRIBUTE_PATTERN = Pattern.compile("\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|\\{([^}]*)})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRESENTATION_ROLE_PATTERN = Pattern.compile("\\brole\\s*=\\s*(?:\"(presentation|none)\"|'(presentation|none)'|\\{[\"'](presentation|none)[\"']})", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findDecorativeImageIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.1.1")
                .name("\uC774\uBBF8\uC9C0 \uB300\uCCB4 \uD14D\uC2A4\uD2B8 - \uC21C\uC218 \uC7A5\uC2DD")
                .level("A")
                .mvpDescription("\uC7A5\uC2DD, \uD3EC\uB9F7\uD305, \uBE44\uD45C\uC2DC \uBE44\uD14D\uC2A4\uD2B8 \uCF58\uD150\uCE20\uB294 \uBCF4\uC870 \uAE30\uC220\uC774 \uBB34\uC2DC\uD560 \uC218 \uC788\uAC8C \uAD6C\uD604\uD55C\uB2E4.")
                .implementationMethod("\uC815\uC801 \uBD84\uC11D")
                .implementationDescription("\uC7A5\uC2DD \uC774\uBBF8\uC9C0\uAC00 alt=\"\" \uB610\uB294 role=\"presentation\"/role=\"none\"\uC73C\uB85C \uBCF4\uC870 \uAE30\uC220\uC5D0\uC11C \uBB34\uC2DC\uB418\uB294\uC9C0 \uAC80\uC0AC\uD55C\uB2E4.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findDecorativeImageIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        Matcher matcher = IMG_TAG_PATTERN.matcher(code);

        while (matcher.find()) {
            String imgTag = matcher.group();
            if (hasEmptyAlt(imgTag) || hasPresentationRole(imgTag)) {
                continue;
            }

            issues.add(buildIssue(
                    "DECORATIVE_IMAGE_NOT_IGNORED",
                    "\uC21C\uC218 \uC7A5\uC2DD \uC774\uBBF8\uC9C0\uB294 alt=\"\" \uB610\uB294 role=\"presentation\"/role=\"none\"\uC73C\uB85C \uBCF4\uC870 \uAE30\uC220\uC774 \uBB34\uC2DC\uD560 \uC218 \uC788\uAC8C \uD574\uC57C \uD569\uB2C8\uB2E4.",
                    code,
                    matcher.start(),
                    imgTag
            ));
        }

        return issues;
    }

    private boolean hasEmptyAlt(String imgTag) {
        Matcher altMatcher = ALT_ATTRIBUTE_PATTERN.matcher(imgTag);
        if (!altMatcher.find()) {
            return false;
        }

        String altValue = firstMatchedGroup(altMatcher, 1, 2, 3);
        return altValue == null || altValue.trim().isEmpty() || "\"\"".equals(altValue.trim()) || "''".equals(altValue.trim());
    }

    private boolean hasPresentationRole(String imgTag) {
        return PRESENTATION_ROLE_PATTERN.matcher(imgTag).find();
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
        if (normalized.length() > 180) {
            return normalized.substring(0, 180) + "...";
        }
        return normalized;
    }

    private String firstMatchedGroup(Matcher matcher, int... groups) {
        for (int group : groups) {
            String value = matcher.group(group);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
