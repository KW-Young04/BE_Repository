package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc111NonTextContentRule implements AccessibilityRule {

    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALT_ATTRIBUTE_PATTERN = Pattern.compile("\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|\\{([^}]*)})", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEDIA_BLOCK_PATTERN = Pattern.compile("<(video|audio)\\b[^>]*>(.*?)</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SELF_CLOSING_MEDIA_PATTERN = Pattern.compile("<(video|audio)\\b[^>]*/>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACK_TAG_PATTERN = Pattern.compile("<track\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findImageAltIssues(code));
            issues.addAll(findMediaTrackIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.1.1")
                .name("\uC774\uBBF8\uC9C0 \uB300\uCCB4 \uD14D\uC2A4\uD2B8 - \uC2DC\uAC04 \uAE30\uBC18 \uBBF8\uB514\uC5B4")
                .level("A")
                .mvpDescription("video, audio\uC778 \uBE44\uD14D\uC2A4\uD2B8 \uCF58\uD150\uCE20\uC5D0 \uC124\uBA85\uC801 \uC2DD\uBCC4 \uD14D\uC2A4\uD2B8 \uB300\uC548\uC744 \uC81C\uACF5\uD55C\uB2E4.")
                .implementationMethod("\uC815\uC801 \uBD84\uC11D")
                .implementationDescription("img \uD0DC\uADF8\uC758 alt \uC18D\uC131\uACFC video/audio \uD0DC\uADF8 \uB0B4\uBD80\uC758 track \uD0DC\uADF8 \uC874\uC7AC \uC5EC\uBD80\uB97C \uAC80\uC0AC\uD55C\uB2E4.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("video/audio 태그 내부에 <track> 요소를 추가해 자막이나 설명 텍스트를 제공하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findImageAltIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        Matcher matcher = IMG_TAG_PATTERN.matcher(code);

        while (matcher.find()) {
            String imgTag = matcher.group();
            Matcher altMatcher = ALT_ATTRIBUTE_PATTERN.matcher(imgTag);

            if (!altMatcher.find()) {
                issues.add(buildIssue("IMG_ALT_MISSING", "img \uD0DC\uADF8\uC5D0 alt \uC18D\uC131\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.", code, matcher.start(), imgTag));
                continue;
            }

            String altValue = firstMatchedGroup(altMatcher, 1, 2, 3);
            if (altValue == null || altValue.trim().isEmpty() || "\"\"".equals(altValue.trim()) || "''".equals(altValue.trim())) {
                issues.add(buildIssue("IMG_ALT_EMPTY", "img \uD0DC\uADF8\uC758 alt \uAC12\uC774 \uBE44\uC5B4 \uC788\uC2B5\uB2C8\uB2E4.", code, matcher.start(), imgTag));
            }
        }

        return issues;
    }

    private List<AccessibilityIssue> findMediaTrackIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        Matcher blockMatcher = MEDIA_BLOCK_PATTERN.matcher(code);

        while (blockMatcher.find()) {
            String tagName = blockMatcher.group(1).toLowerCase();
            String mediaContent = blockMatcher.group(2);

            if (!TRACK_TAG_PATTERN.matcher(mediaContent).find()) {
                issues.add(buildIssue("MEDIA_TRACK_MISSING", tagName + " \uD0DC\uADF8 \uB0B4\uBD80\uC5D0 track \uD0DC\uADF8\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.", code, blockMatcher.start(), blockMatcher.group()));
            }
        }

        Matcher selfClosingMatcher = SELF_CLOSING_MEDIA_PATTERN.matcher(code);
        while (selfClosingMatcher.find()) {
            String tagName = selfClosingMatcher.group(1).toLowerCase();
            issues.add(buildIssue("MEDIA_TRACK_MISSING", tagName + " \uD0DC\uADF8 \uB0B4\uBD80\uC5D0 track \uD0DC\uADF8\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.", code, selfClosingMatcher.start(), selfClosingMatcher.group()));
        }

        return issues;
    }

    private AccessibilityIssue buildIssue(String type, String message, String code, int startIndex, String snippet) {
        int lineNumber = calculateLineNumber(code, startIndex);
        return AccessibilityIssue.builder()
                .code(type)
                .message(message)
                .startLine(lineNumber)
                .endLine(lineNumber)
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

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(1L);
    }
}
