package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc311PageLanguageRule implements AccessibilityRule {

    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern LANG_ATTRIBUTE_PATTERN =
            Pattern.compile("\\blang\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE);

    private static final Set<String> COMMON_LANGUAGE_CODES = Set.of(
            "ko", "ko-KR",
            "en", "en-US", "en-GB",
            "ja", "ja-JP",
            "zh", "zh-CN", "zh-TW",
            "fr", "de", "es", "it", "pt", "ru"
    );

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        boolean hasHtmlTag = false;
        boolean hasLang = false;
        boolean validLang = false;

        if (code != null && !code.isBlank()) {
            Matcher htmlMatcher = HTML_TAG_PATTERN.matcher(code);

            if (htmlMatcher.find()) {
                hasHtmlTag = true;
                String htmlTag = htmlMatcher.group();

                Matcher langMatcher = LANG_ATTRIBUTE_PATTERN.matcher(htmlTag);
                if (langMatcher.find()) {
                    hasLang = true;
                    String lang = getLangValue(langMatcher);

                    if (lang == null || lang.isBlank()) {
                        issues.add(buildIssue(
                                "PAGE_LANGUAGE_EMPTY",
                                "html lang 속성이 비어 있습니다.",
                                code,
                                htmlMatcher.start(),
                                htmlTag
                        ));
                    } else if (isValidLanguageCode(lang)) {
                        validLang = true;
                    } else {
                        issues.add(buildIssue(
                                "PAGE_LANGUAGE_INVALID",
                                "html lang 속성이 유효한 BCP 47 언어 코드로 보기 어렵습니다.",
                                code,
                                htmlMatcher.start(),
                                htmlTag
                        ));
                    }
                }
            }
        }

        if (!hasHtmlTag) {
            issues.add(buildIssue(
                    "HTML_TAG_MISSING",
                    "html 태그를 찾을 수 없습니다.",
                    code,
                    0,
                    "html 태그 미검출"
            ));
        } else if (!hasLang) {
            issues.add(buildIssue(
                    "PAGE_LANGUAGE_MISSING",
                    "html 태그에 lang 속성이 없습니다.",
                    code,
                    0,
                    "lang 속성 미검출"
            ));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("3.1.1")
                .name("페이지 언어")
                .level("A")
                .mvpDescription("각 웹 페이지의 기본 인간 언어를 프로그래밍적으로 확인할 수 있어야 합니다.")
                .implementationMethod("코드")
                .implementationDescription("html 태그의 lang 속성 존재 여부와 유효한 언어 코드 여부를 확인합니다.")
                .status(issues.isEmpty() && validLang ? "PASS" : "FAIL")
                .aiReviewRequired(false)
                .issues(issues)
                .build();
    }

    private String getLangValue(Matcher matcher) {
        if (matcher.group(1) != null) {
            return matcher.group(1).trim();
        }
        if (matcher.group(2) != null) {
            return matcher.group(2).trim();
        }
        return null;
    }

    private boolean isValidLanguageCode(String lang) {
        if (COMMON_LANGUAGE_CODES.contains(lang)) {
            return true;
        }

        try {
            Locale.forLanguageTag(lang);
            return !Locale.forLanguageTag(lang).getLanguage().isBlank();
        } catch (Exception e) {
            return false;
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