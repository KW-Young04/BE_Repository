package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc332LabelsOrInstructionsRule implements AccessibilityRule {

    private static final Pattern FORM_CONTROL_PATTERN =
            Pattern.compile("<(?:input|textarea|select)\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern TYPE_HIDDEN_PATTERN =
            Pattern.compile("\\btype\\s*=\\s*[\"']hidden[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern ID_PATTERN =
            Pattern.compile("\\bid\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)')", Pattern.CASE_INSENSITIVE);

    private static final Pattern ACCESSIBLE_LABEL_PATTERN =
            Pattern.compile("\\b(?:aria-label|aria-labelledby|aria-describedby|placeholder|title)\\s*=", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            Matcher matcher = FORM_CONTROL_PATTERN.matcher(code);

            while (matcher.find()) {
                String control = matcher.group();

                if (TYPE_HIDDEN_PATTERN.matcher(control).find()) {
                    continue;
                }

                if (hasAccessibleLabel(code, control)) {
                    continue;
                }

                issues.add(buildIssue(
                        "LABEL_OR_DESCRIPTION_MISSING",
                        "사용자 입력 요소에 label, placeholder, aria-label, aria-describedby 등의 레이블 또는 설명이 없습니다.",
                        code,
                        matcher.start(),
                        control
                ));
            }
        }

        boolean aiReviewRequired = issues.isEmpty();

        return AccessibilityCheckResult.builder()
                .successCriteria("3.3.2")
                .name("레이블 또는 설명")
                .level("A")
                .mvpDescription("사용자 입력이 필요한 콘텐츠에는 레이블 또는 설명을 제공해야 합니다.")
                .implementationMethod("코드")
                .implementationDescription("label-for 연결, placeholder, aria-label, aria-labelledby, aria-describedby 존재 여부를 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "레이블 또는 설명이 실제 입력 목적을 충분히 설명하는지 AI 검토가 가능합니다." : null)
                .issues(issues)
                .build();
    }

    private boolean hasAccessibleLabel(String fullCode, String control) {
        if (ACCESSIBLE_LABEL_PATTERN.matcher(control).find()) {
            return true;
        }

        String id = extractId(control);
        if (id == null || id.isBlank()) {
            return false;
        }

        Pattern labelForPattern = Pattern.compile("<label\\b[^>]*for\\s*=\\s*(?:\"" + Pattern.quote(id) + "\"|'" + Pattern.quote(id) + "')[^>]*>",
                Pattern.CASE_INSENSITIVE);

        return labelForPattern.matcher(fullCode).find();
    }

    private String extractId(String control) {
        Matcher matcher = ID_PATTERN.matcher(control);
        if (matcher.find()) {
            if (matcher.group(1) != null) {
                return matcher.group(1);
            }
            if (matcher.group(2) != null) {
                return matcher.group(2);
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