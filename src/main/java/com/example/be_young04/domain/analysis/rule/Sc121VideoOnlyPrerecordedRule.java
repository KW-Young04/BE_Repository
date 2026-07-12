package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc121VideoOnlyPrerecordedRule implements AccessibilityRule {

    private static final Pattern VIDEO_BLOCK_PATTERN = Pattern.compile("<video\\b[^>]*>(.*?)</video>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SELF_CLOSING_VIDEO_PATTERN = Pattern.compile("<video\\b[^>]*/>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTIONS_TRACK_PATTERN = Pattern.compile("<track\\b(?=[^>]*\\bkind\\s*=\\s*(?:\"descriptions\"|'descriptions'|\\{[\"']descriptions[\"']}))[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSCRIPT_LINK_PATTERN = Pattern.compile("<a\\b[^>]*href\\s*=\\s*(?:\"[^\"]+\"|'[^']+'|\\{[^}]+})[^>]*>.*?(?:transcript|script|description|descriptions|\\uB300\\uBCF8|\\uC124\\uBA85|\\uD574\\uC124|\\uD14D\\uC2A4\\uD2B8).*?</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int ADJACENT_TEXT_WINDOW = 500;

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findVideoAlternativeIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.2.1")
                .name("\uBE44\uB514\uC624 \uC804\uC6A9(\uC0AC\uC804\uB179\uC74C) - \uB300\uC548 \uC81C\uACF5")
                .level("A")
                .mvpDescription("\uC0AC\uC804\uB179\uC74C \uBE44\uB514\uC624 \uC804\uC6A9 \uCF58\uD150\uCE20\uC5D0 \uD14D\uC2A4\uD2B8 \uB300\uC548 \uB610\uB294 \uC624\uB514\uC624 \uD2B8\uB799\uC744 \uC81C\uACF5\uD55C\uB2E4.")
                .implementationMethod("\uC815\uC801 \uBD84\uC11D")
                .implementationDescription("video \uD0DC\uADF8\uAC00 \uC788\uC744 \uB54C track kind=\"descriptions\" \uB610\uB294 \uC778\uC811\uD55C \uD14D\uC2A4\uD2B8 \uB300\uBCF8 \uB9C1\uD06C\uAC00 \uC788\uB294\uC9C0 \uAC80\uC0AC\uD55C\uB2E4.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("비디오 콘텐츠에 텍스트 대안이나 오디오 트랙을 추가하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findVideoAlternativeIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        Matcher blockMatcher = VIDEO_BLOCK_PATTERN.matcher(code);

        while (blockMatcher.find()) {
            String videoBlock = blockMatcher.group();
            if (hasDescriptionsTrack(videoBlock) || hasAdjacentTranscriptLink(code, blockMatcher.start(), blockMatcher.end())) {
                continue;
            }

            issues.add(buildIssue(
                    "VIDEO_ALTERNATIVE_MISSING",
                    "\uC0AC\uC804\uB179\uC74C \uBE44\uB514\uC624 \uC804\uC6A9 \uCF58\uD150\uCE20\uC5D0 track kind=\"descriptions\" \uB610\uB294 \uC778\uC811\uD55C \uD14D\uC2A4\uD2B8 \uB300\uBCF8 \uB9C1\uD06C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.",
                    code,
                    blockMatcher.start(),
                    videoBlock
            ));
        }

        Matcher selfClosingMatcher = SELF_CLOSING_VIDEO_PATTERN.matcher(code);
        while (selfClosingMatcher.find()) {
            String videoTag = selfClosingMatcher.group();
            if (hasAdjacentTranscriptLink(code, selfClosingMatcher.start(), selfClosingMatcher.end())) {
                continue;
            }

            issues.add(buildIssue(
                    "VIDEO_ALTERNATIVE_MISSING",
                    "\uC0AC\uC804\uB179\uC74C \uBE44\uB514\uC624 \uC804\uC6A9 \uCF58\uD150\uCE20\uC5D0 track kind=\"descriptions\" \uB610\uB294 \uC778\uC811\uD55C \uD14D\uC2A4\uD2B8 \uB300\uBCF8 \uB9C1\uD06C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.",
                    code,
                    selfClosingMatcher.start(),
                    videoTag
            ));
        }

        return issues;
    }

    private boolean hasDescriptionsTrack(String videoBlock) {
        return DESCRIPTIONS_TRACK_PATTERN.matcher(videoBlock).find();
    }

    private boolean hasAdjacentTranscriptLink(String code, int startIndex, int endIndex) {
        int windowStart = Math.max(0, startIndex - ADJACENT_TEXT_WINDOW);
        int windowEnd = Math.min(code.length(), endIndex + ADJACENT_TEXT_WINDOW);
        String adjacentText = code.substring(windowStart, windowEnd);
        return TRANSCRIPT_LINK_PATTERN.matcher(adjacentText).find();
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

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(8L);
    }
}
