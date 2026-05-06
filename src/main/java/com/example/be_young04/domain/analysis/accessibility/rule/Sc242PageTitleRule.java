package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc242PageTitleRule implements AccessibilityRule {

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<title\\b[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        boolean titleExists = false;
        boolean titleNotEmpty = false;

        if (code != null && !code.isBlank()) {
            Matcher matcher = TITLE_PATTERN.matcher(code);

            if (matcher.find()) {
                titleExists = true;
                String titleText = matcher.group(1).replaceAll("\\s+", " ").trim();

                if (!titleText.isBlank()) {
                    titleNotEmpty = true;
                } else {
                    issues.add(buildIssue(
                            "PAGE_TITLE_EMPTY",
                            "title 태그가 존재하지만 비어 있습니다.",
                            code,
                            matcher.start(),
                            matcher.group()
                    ));
                }
            }
        }

        if (!titleExists) {
            issues.add(buildIssue(
                    "PAGE_TITLE_MISSING",
                    "페이지의 주제나 목적을 설명하는 title 태그가 없습니다.",
                    code,
                    0,
                    "title 태그 미검출"
            ));
        }

        boolean aiReviewRequired = issues.isEmpty() && titleNotEmpty;

        return AccessibilityCheckResult.builder()
                .successCriteria("2.4.2")
                .name("페이지 제목")
                .level("A")
                .mvpDescription("웹 페이지에는 주제나 목적을 설명하는 title 태그가 존재해야 합니다.")
                .implementationMethod("코드")
                .implementationDescription("title 태그 존재 여부와 비어 있지 않은지 여부를 코드로 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "title 내용이 페이지의 주제나 목적을 충분히 설명하는지 AI 검토가 가능합니다." : null)
                .issues(issues)
                .build();
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
        if (code == null) {
            return 1;
        }

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