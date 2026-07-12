package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 3.2.2 입력 시 변경 없음
 * 입력값 변경 시 자동으로 컨텍스트 변경 금지 (사전 고지 예외)
 */
public class Sc322InputChangeRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findInputChangeIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("3.2.2")
                .name("입력 시 변경 없음")
                .level("A")
                .mvpDescription("입력값 변경 시 자동으로 컨텍스트가 변경되지 않습니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("onchange 이벤트에 즉시 form submit·페이지 이동 패턴을 감지합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findInputChangeIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // onchange에서 즉시 form submit 패턴 감지
        if (Pattern.compile("onChange\\s*=.*?(submit|navigate|redirect)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("INPUT_CHANGE_SUBMIT")
                    .message("onChange 이벤트에서 즉시 form이 제출됩니다. 사전 고지가 필요합니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        // select 요소에서 onchange로 페이지 이동
        if (Pattern.compile("<select[^>]*onChange\\s*=.*?(window\\.location|navigate)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("SELECT_CHANGE_NAVIGATION")
                    .message("select 요소의 onChange에서 페이지 이동이 발생합니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        return issues;
    }
}
