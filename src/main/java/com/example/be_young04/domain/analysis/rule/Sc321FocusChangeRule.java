package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 3.2.1 포커스 시 변경 없음
 * 포커스 시 컨텍스트 변경 발생 금지
 */
public class Sc321FocusChangeRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findFocusChangeIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("3.2.1")
                .name("포커스 시 변경 없음")
                .level("A")
                .mvpDescription("포커스 시 예기치 않은 컨텍스트 변경이 발생하지 않습니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("focus 이벤트에 페이지 이동·팝업 열기 등 컨텍스트 변경 코드 패턴을 감지합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .defaultSuggestion("onFocus 이벤트에서 페이지 이동이나 팝업이 자동으로 열리지 않도록 하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findFocusChangeIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // focus 이벤트에 페이지 이동 패턴 감지
        if (Pattern.compile("onFocus\\s*=.*?(window\\.location|navigate|redirect)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("FOCUS_NAVIGATION_TRIGGER")
                    .message("focus 이벤트에서 페이지 이동이 발생합니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        // focus 이벤트에 팝업 열기 패턴 감지
        if (Pattern.compile("onFocus\\s*=.*?(showModal|openDialog|alert|confirm)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("FOCUS_MODAL_TRIGGER")
                    .message("focus 이벤트에서 팝업이나 모달이 열립니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(66L);
    }
}
