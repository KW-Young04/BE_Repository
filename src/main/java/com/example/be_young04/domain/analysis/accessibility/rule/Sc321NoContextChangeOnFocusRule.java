package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc321NoContextChangeOnFocusRule implements AccessibilityRule {

    private static final Pattern ONFOCUS_PATTERN =
            Pattern.compile("\\bonFocus\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|\\{([^}]*)})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FOCUS_CONTEXT_CHANGE_PATTERN =
            Pattern.compile("location\\.|window\\.location|window\\.open|\\.submit\\s*\\(|navigate\\s*\\(|router\\.push|href\\s*=",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        boolean hasFocusEvent = false;

        if (code != null && !code.isBlank()) {
            Matcher matcher = ONFOCUS_PATTERN.matcher(code);

            while (matcher.find()) {
                hasFocusEvent = true;
                String handler = getHandlerValue(matcher);

                if (handler != null && FOCUS_CONTEXT_CHANGE_PATTERN.matcher(handler).find()) {
                    issues.add(buildIssue(
                            "CONTEXT_CHANGE_ON_FOCUS",
                            "포커스 이벤트에서 페이지 이동, 새 창 열기, form submit 등 컨텍스트 변경 가능성이 감지되었습니다.",
                            code,
                            matcher.start(),
                            matcher.group()
                    ));
                }
            }
        }

        boolean aiReviewRequired = issues.isEmpty() && hasFocusEvent;

        return AccessibilityCheckResult.builder()
                .successCriteria("3.2.1")
                .name("포커스 시 변경 없음")
                .level("A")
                .mvpDescription("포커스를 받은 것만으로 사용자에게 예측 불가능한 컨텍스트 변경이 발생하면 안 됩니다.")
                .implementationMethod("코드")
                .implementationDescription("onFocus 이벤트에서 페이지 이동, 파일 열기, form submit 등 컨텍스트 변경 코드 패턴을 탐지합니다.")
                .status(issues.isEmpty() ? (aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS") : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "onFocus 이벤트가 실제로 사용자에게 예측 불가능한 컨텍스트 변경을 일으키는지 검토해야 합니다." : null)
                .issues(issues)
                .build();
    }

    private String getHandlerValue(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) {
                return matcher.group(i);
            }
        }
        return null;
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