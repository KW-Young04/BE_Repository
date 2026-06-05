package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SC 3.1.1 페이지 언어
 * 각 웹 페이지의 기본 인간 언어를 프로그래밍적으로 확인 가능
 */
public class Sc311PageLanguageRule implements AccessibilityRule {

    private static final Pattern HTML_LANG_PATTERN = Pattern.compile("<html\\b[^>]*lang\\s*=\\s*['\"]([a-z]{2}(?:-[a-z]{2})?)['\"]", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findLanguageIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("3.1.1")
                .name("페이지 언어")
                .level("A")
                .mvpDescription("웹 페이지의 기본 언어를 프로그래밍적으로 확인 가능하게 지정합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("html 요소의 lang 속성 존재 여부와 유효한 BCP 47 언어 코드 여부를 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findLanguageIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        Matcher langMatcher = HTML_LANG_PATTERN.matcher(code);
        if (!langMatcher.find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("PAGE_LANGUAGE_MISSING")
                    .message("html 태그에 lang 속성이 없습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(300, code.length())))
                    .build());
        } else {
            String langCode = langMatcher.group(1);
            // 유효한 BCP 47 언어 코드 확인
            if (!isValidLanguageCode(langCode)) {
                issues.add(AccessibilityIssue.builder()
                        .code("INVALID_LANGUAGE_CODE")
                        .message("lang 속성 값 '" + langCode + "'이 유효한 BCP 47 언어 코드가 아닙니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(langMatcher.group())
                        .build());
            }
        }

        return issues;
    }

    private boolean isValidLanguageCode(String code) {
        // BCP 47 기본 언어 코드 검증 (en, ko, ja, zh 등)
        return Pattern.compile("^[a-z]{2}(?:-[a-z]{2})?$", Pattern.CASE_INSENSITIVE).matcher(code).matches();
    }
}
