package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SC 2.4.2 페이지 제목
 * 웹 페이지에 주제나 목적을 설명하는 title 태그 존재
 */
public class Sc242PageTitleRule implements AccessibilityRule {

    private static final Pattern TITLE_TAG_PATTERN = Pattern.compile("<title\\b[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findTitleIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.4.2")
                .name("페이지 제목")
                .level("A")
                .mvpDescription("웹 페이지의 주제나 목적을 설명하는 title 태그를 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("title 태그의 존재 여부와 비어있지 않은지 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("페이지의 주제를 설명하는 <title> 태그를 추가하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findTitleIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        Matcher titleMatcher = TITLE_TAG_PATTERN.matcher(code);
        if (!titleMatcher.find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("PAGE_TITLE_MISSING")
                    .message("title 태그가 없습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(300, code.length())))
                    .build());
        } else {
            String titleContent = titleMatcher.group(1).trim();
            if (titleContent.isEmpty()) {
                issues.add(AccessibilityIssue.builder()
                        .code("PAGE_TITLE_EMPTY")
                        .message("title 태그가 비어있습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(titleMatcher.group())
                        .build());
            }
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(51L);
    }

    @Override
    public boolean isRootDocumentOnly() {
        return true;
    }
}
