package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 4.1.2 이름·역할·값
 * 표준 HTML 컨트롤은 명세에 따라 사용 시 이미 충족
 * 커스텀 컴포넌트의 ARIA role·name·state 분석
 */
public class Sc412NameRoleValueRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findNameRoleValueIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("4.1.2")
                .name("이름·역할·값")
                .level("A")
                .mvpDescription("표준 HTML 컨트롤 또는 커스텀 컴포넌트가 이름·역할·값을 명확히 제공합니다.")
                .implementationMethod("코드 + AI 분석")
                .implementationDescription("커스텀 컴포넌트의 ARIA role·name·state를 분석하고 적절성은 AI로 판단합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .aiReviewRequired(true)
                .aiReviewGuide("커스텀 컴포넌트의 ARIA 설정 적절성은 AI 분석이 필요합니다.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findNameRoleValueIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // div/span으로 커스텀 버튼 만들기 (role 없음)
        if (Pattern.compile("<(div|span)\\b[^>]*\\bonclick", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("role\\s*=\\s*['\"]button['\"]", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("CUSTOM_BUTTON_NO_ROLE")
                        .message("div/span에 onclick이 있지만 role='button'이 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        // role 사용하지만 aria-label 없음
        if (Pattern.compile("role\\s*=", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("aria-label|aria-labelledby", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                // 표준 텍스트 콘텐츠도 없는 경우
                if (!Pattern.compile(">\\s*[a-zA-Z0-9가-힣]", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                    issues.add(AccessibilityIssue.builder()
                            .code("CUSTOM_COMPONENT_NO_NAME")
                            .message("role이 있지만 accessible name(aria-label 또는 텍스트 콘텐츠)이 없습니다.")
                            .startLine(0)
                            .endLine(0)
                            .snippet(code.substring(0, Math.min(400, code.length())))
                            .build());
                }
            }
        }

        // aria-pressed, aria-checked 등 상태 속성 확인
        if (Pattern.compile("role\\s*=\\s*['\"]?(button|checkbox|switch)", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("aria-pressed|aria-checked|aria-selected", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("CUSTOM_COMPONENT_NO_STATE")
                        .message("상태 변화 컴포넌트(button/checkbox/switch)가 aria-pressed/aria-checked/aria-selected 없이 상태를 관리하고 있습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        return issues;
    }
}
