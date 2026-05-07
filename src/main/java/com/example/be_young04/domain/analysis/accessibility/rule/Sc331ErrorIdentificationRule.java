package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc331ErrorIdentificationRule implements AccessibilityRule {

    private static final Pattern INPUT_PATTERN =
            Pattern.compile("<(?:input|textarea|select)\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern REQUIRED_OR_INVALID_PATTERN =
            Pattern.compile("\\b(required|aria-invalid\\s*=\\s*[\"']true[\"'])\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern ERROR_MESSAGE_PATTERN =
            Pattern.compile("error|invalid|오류|잘못|필수|입력하세요|확인해 주세요", Pattern.CASE_INSENSITIVE);

    private static final Pattern ROLE_ALERT_PATTERN =
            Pattern.compile("role\\s*=\\s*[\"']alert[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern ARIA_DESCRIBEDBY_PATTERN =
            Pattern.compile("aria-describedby\\s*=", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        boolean hasErrorTarget = false;
        boolean hasErrorMessage = false;
        boolean hasRoleAlert = false;
        boolean hasAriaDescribedBy = false;

        if (code != null && !code.isBlank()) {
            hasErrorMessage = ERROR_MESSAGE_PATTERN.matcher(code).find();
            hasRoleAlert = ROLE_ALERT_PATTERN.matcher(code).find();
            hasAriaDescribedBy = ARIA_DESCRIBEDBY_PATTERN.matcher(code).find();

            Matcher matcher = INPUT_PATTERN.matcher(code);
            while (matcher.find()) {
                String input = matcher.group();

                if (REQUIRED_OR_INVALID_PATTERN.matcher(input).find()) {
                    hasErrorTarget = true;

                    if (!hasErrorMessage && !hasAriaDescribedBy) {
                        issues.add(buildIssue(
                                "ERROR_IDENTIFICATION_MISSING",
                                "입력 오류 가능성이 있는 필드에 오류 식별 메시지 또는 aria-describedby 연결이 없습니다.",
                                code,
                                matcher.start(),
                                input
                        ));
                    }
                }
            }
        }

        boolean aiReviewRequired = issues.isEmpty() && (hasErrorTarget || hasErrorMessage || hasRoleAlert || hasAriaDescribedBy);

        return AccessibilityCheckResult.builder()
                .successCriteria("3.3.1")
                .name("오류 식별")
                .level("A")
                .mvpDescription("입력 오류가 자동 감지되면 오류 항목을 식별하고 사용자에게 텍스트로 설명해야 합니다.")
                .implementationMethod("코드+AI")
                .implementationDescription("required, aria-invalid, role=alert, aria-describedby, 오류 메시지 패턴을 코드로 확인하고 메시지 적절성은 AI로 검토합니다.")
                .status(issues.isEmpty() ? (aiReviewRequired ? "NEEDS_AI_REVIEW" : "PASS") : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "오류 메시지가 실제로 어떤 항목의 어떤 문제인지 명확하게 설명하는지 AI 검토가 필요합니다." : null)
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