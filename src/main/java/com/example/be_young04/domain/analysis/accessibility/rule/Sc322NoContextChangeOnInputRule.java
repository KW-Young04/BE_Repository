package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc322NoContextChangeOnInputRule implements AccessibilityRule {

    private static final Pattern ONCHANGE_PATTERN =
            Pattern.compile("\\bonChange\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|\\{([^}]*)})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern INPUT_CONTEXT_CHANGE_PATTERN =
            Pattern.compile("location\\.|window\\.location|window\\.open|\\.submit\\s*\\(|navigate\\s*\\(|router\\.push|form\\.submit",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PRIOR_NOTICE_PATTERN =
            Pattern.compile("사전\\s*고지|변경\\s*시\\s*이동|선택\\s*시\\s*이동|change\\s*will\\s*navigate|submit\\s*automatically",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        boolean hasChangeEvent = false;
        boolean hasPriorNotice = false;

        if (code != null && !code.isBlank()) {
            hasPriorNotice = PRIOR_NOTICE_PATTERN.matcher(code).find();

            Matcher matcher = ONCHANGE_PATTERN.matcher(code);

            while (matcher.find()) {
                hasChangeEvent = true;
                String handler = getHandlerValue(matcher);

                if (handler != null && INPUT_CONTEXT_CHANGE_PATTERN.matcher(handler).find() && !hasPriorNotice) {
                    issues.add(buildIssue(
                            "CONTEXT_CHANGE_ON_INPUT",
                            "입력값 변경 이벤트에서 즉시 form submit, 페이지 이동, 새 창 열기 등 컨텍스트 변경 가능성이 감지되었습니다.",
                            code,
                            matcher.start(),
                            matcher.group()
                    ));
                }
            }
        }

        boolean aiReviewRequired = issues.isEmpty() && hasChangeEvent;

        return AccessibilityCheckResult.builder()
                .successCriteria("3.2.2")
                .name("입력 시 변경 없음")
                .level("A")
                .mvpDescription("사용자 입력값 변경만으로 예측 불가능한 컨텍스트 변경이 발생하면 안 됩니다.")
                .implementationMethod("코드")
                .implementationDescription("onChange 이벤트에서 즉시 form submit, 페이지 이동, 새 창 열기 등 컨텍스트 변경 패턴을 탐지합니다.")
                .status(issues.isEmpty() ? (aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS") : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "입력값 변경 시 발생하는 동작이 사전 고지되었거나 사용자가 예측 가능한 동작인지 검토해야 합니다." : null)
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