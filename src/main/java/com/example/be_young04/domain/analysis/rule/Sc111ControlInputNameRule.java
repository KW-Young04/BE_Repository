package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc111ControlInputNameRule implements AccessibilityRule {

    private static final Pattern INPUT_IMAGE_PATTERN = Pattern.compile("<input\\b(?=[^>]*\\btype\\s*=\\s*(?:\"image\"|'image'|\\{[\"']image[\"']}))[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICKABLE_IMG_PATTERN = Pattern.compile("<img\\b(?=[^>]*(?:\\bonClick\\s*=|\\brole\\s*=\\s*(?:\"(?:button|link)\"|'(?:button|link)'|\\{[\"'](?:button|link)[\"']})))[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUTTON_OR_LINK_WITH_IMAGE_PATTERN = Pattern.compile("<(button|a)\\b[^>]*>.*?<img\\b[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CONTROL_SVG_PATTERN = Pattern.compile("<svg\\b(?=[^>]*(?:\\bonClick\\s*=|\\brole\\s*=\\s*(?:\"(?:button|link)\"|'(?:button|link)'|\\{[\"'](?:button|link)[\"']})))[^>]*>(.*?)</svg>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern ACCESSIBLE_NAME_ATTRIBUTE_PATTERN = Pattern.compile("\\b(?:aria-label|aria-labelledby|title|alt)\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_TITLE_PATTERN = Pattern.compile("<title\\b[^>]*>\\s*[^<\\s][^<]*</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        int namedControlCount = 0;

        if (code != null && !code.isBlank()) {
            AnalysisCounter counter = findControlNameIssues(code, issues);
            namedControlCount = counter.namedControlCount();
        }

        boolean aiReviewRequired = issues.isEmpty() && namedControlCount > 0;

        return AccessibilityCheckResult.builder()
                .successCriteria("1.1.1")
                .name("\uC774\uBBF8\uC9C0 \uB300\uCCB4 \uD14D\uC2A4\uD2B8 - \uCEE8\uD2B8\uB864/\uC785\uB825")
                .level("A")
                .mvpDescription("\uC785\uB825\uC744 \uBC1B\uAC70\uB098 \uCEE8\uD2B8\uB864 \uC5ED\uD560\uC744 \uD558\uB294 \uBE44\uD14D\uC2A4\uD2B8 \uCF58\uD150\uCE20\uC5D0 \uBAA9\uC801\uC744 \uC124\uBA85\uD558\uB294 name\uC774 \uC788\uC5B4\uC57C \uD55C\uB2E4.")
                .implementationMethod("\uCF54\uB4DC+AI")
                .implementationDescription("aria-label, aria-labelledby, title, alt \uC18D\uC131 \uC874\uC7AC\uB294 \uCF54\uB4DC\uB85C \uD655\uC778\uD558\uACE0, name\uC774 \uCEE8\uD2B8\uB864\uC758 \uBAA9\uC801\uC744 \uC2E4\uC81C\uB85C \uC124\uBA85\uD558\uB294\uC9C0\uB294 AI \uD310\uB2E8\uC774 \uD544\uC694\uD558\uB2E4.")
                .status(issues.isEmpty() ? (aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS") : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "\uC874\uC7AC\uD558\uB294 accessible name\uC774 \uCEE8\uD2B8\uB864\uC758 \uBAA9\uC801\uC744 \uAD6C\uCCB4\uC801\uC73C\uB85C \uC124\uBA85\uD558\uB294\uC9C0 AI\uB85C \uAC80\uD1A0\uD574\uC57C \uD569\uB2C8\uB2E4." : null)
                .issues(issues)
                .build();
    }

    private AnalysisCounter findControlNameIssues(String code, List<AccessibilityIssue> issues) {
        int namedControlCount = 0;

        namedControlCount += inspectSimpleControls(code, issues, INPUT_IMAGE_PATTERN, "CONTROL_INPUT_NAME_MISSING", "\uC774\uBBF8\uC9C0 \uC785\uB825 \uCEE8\uD2B8\uB864\uC5D0 \uBAA9\uC801\uC744 \uC124\uBA85\uD558\uB294 accessible name\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.");
        namedControlCount += inspectSimpleControls(code, issues, CLICKABLE_IMG_PATTERN, "CONTROL_IMAGE_NAME_MISSING", "\uCEE8\uD2B8\uB864 \uC5ED\uD560\uC758 img\uC5D0 \uBAA9\uC801\uC744 \uC124\uBA85\uD558\uB294 accessible name\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.");
        namedControlCount += inspectButtonOrLinkImages(code, issues);
        namedControlCount += inspectControlSvgs(code, issues);

        return new AnalysisCounter(namedControlCount);
    }

    private int inspectSimpleControls(String code, List<AccessibilityIssue> issues, Pattern pattern, String type, String message) {
        int namedControlCount = 0;
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String tag = matcher.group();
            if (hasAccessibleName(tag)) {
                namedControlCount++;
                continue;
            }

            issues.add(buildIssue(type, message, code, matcher.start(), tag));
        }

        return namedControlCount;
    }

    private int inspectButtonOrLinkImages(String code, List<AccessibilityIssue> issues) {
        int namedControlCount = 0;
        Matcher matcher = BUTTON_OR_LINK_WITH_IMAGE_PATTERN.matcher(code);

        while (matcher.find()) {
            String block = matcher.group();
            if (hasAccessibleName(block)) {
                namedControlCount++;
                continue;
            }

            issues.add(buildIssue(
                    "CONTROL_IMAGE_NAME_MISSING",
                    "\uC774\uBBF8\uC9C0\uB9CC \uD3EC\uD568\uD55C \uB9C1\uD06C/\uBC84\uD2BC\uC5D0 \uBAA9\uC801\uC744 \uC124\uBA85\uD558\uB294 accessible name\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.",
                    code,
                    matcher.start(),
                    block
            ));
        }

        return namedControlCount;
    }

    private int inspectControlSvgs(String code, List<AccessibilityIssue> issues) {
        int namedControlCount = 0;
        Matcher matcher = CONTROL_SVG_PATTERN.matcher(code);

        while (matcher.find()) {
            String svgBlock = matcher.group();
            if (hasAccessibleName(svgBlock) || SVG_TITLE_PATTERN.matcher(svgBlock).find()) {
                namedControlCount++;
                continue;
            }

            issues.add(buildIssue(
                    "CONTROL_SVG_NAME_MISSING",
                    "\uCEE8\uD2B8\uB864 \uC5ED\uD560\uC758 svg\uC5D0 \uBAA9\uC801\uC744 \uC124\uBA85\uD558\uB294 accessible name\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.",
                    code,
                    matcher.start(),
                    svgBlock
            ));
        }

        return namedControlCount;
    }

    private boolean hasAccessibleName(String markup) {
        return ACCESSIBLE_NAME_ATTRIBUTE_PATTERN.matcher(markup).find();
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

    private record AnalysisCounter(int namedControlCount) {
    }
}
