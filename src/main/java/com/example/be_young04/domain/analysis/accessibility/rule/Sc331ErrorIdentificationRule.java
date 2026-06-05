package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 3.3.1 오류 식별
 * 입력 오류 자동 감지 시 오류 항목 식별 및 텍스트로 설명
 */
public class Sc331ErrorIdentificationRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findErrorIdentificationIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("3.3.1")
                .name("오류 식별")
                .level("A")
                .mvpDescription("입력 오류를 자동 감지하여 오류 항목을 식별하고 텍스트로 설명합니다.")
                .implementationMethod("코드 + AI 분석")
                .implementationDescription("role=alert·aria-describedby 코드를 확인하고 오류 메시지 적절성은 AI로 판단합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .aiReviewRequired(true)
                .aiReviewGuide("오류 메시지의 명확성과 정확성은 AI 분석이 필요합니다.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findErrorIdentificationIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // role=alert 미사용 감지
        if (!Pattern.compile("role\\s*=\\s*['\"]alert['\"]", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (Pattern.compile("(error|validation|invalid)", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("ERROR_ALERT_MISSING")
                        .message("오류 메시지가 있지만 role='alert'가 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        // aria-describedby 미사용 감지
        if (!Pattern.compile("aria-describedby", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (Pattern.compile("(error-message|validation-error)", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("ERROR_DESCRIPTION_MISSING")
                        .message("오류 메시지와 필드를 연결하는 aria-describedby가 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        return issues;
    }
}
