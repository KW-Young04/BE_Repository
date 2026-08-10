package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 3.3.2 레이블 또는 설명
 * 사용자 입력 필요 콘텐츠에 레이블·설명 제공
 */
public class Sc332LabelDescriptionRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findLabelDescriptionIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("3.3.2")
                .name("레이블 또는 설명")
                .level("A")
                .mvpDescription("사용자 입력이 필요한 모든 콘텐츠에 레이블이나 설명을 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("input 요소에 label·placeholder·aria-label·aria-describedby 존재 여부를 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("input/textarea/select 요소에 label, aria-label, placeholder 중 하나를 추가하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findLabelDescriptionIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // input 요소 감지
        if (Pattern.compile("<input\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            // label 미사용 감지
            if (!Pattern.compile("<label\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                // aria-label, aria-labelledby, placeholder 확인
                if (!Pattern.compile("aria-label|aria-labelledby|placeholder", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                    issues.add(AccessibilityIssue.builder()
                            .code("INPUT_LABEL_MISSING")
                            .message("input 요소에 label, aria-label, aria-labelledby, placeholder 중 하나가 없습니다.")
                            .startLine(0)
                            .endLine(0)
                            .snippet(code.substring(0, Math.min(400, code.length())))
                            .build());
                }
            }
        }

        // textarea 요소 확인
        if (Pattern.compile("<textarea\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("<label\\b[^>]*>.*?</label>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
                if (!Pattern.compile("aria-label|aria-labelledby", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                    issues.add(AccessibilityIssue.builder()
                            .code("TEXTAREA_LABEL_MISSING")
                            .message("textarea 요소에 label이나 aria-label이 없습니다.")
                            .startLine(0)
                            .endLine(0)
                            .snippet(code.substring(0, Math.min(400, code.length())))
                            .build());
                }
            }
        }

        // select 요소 확인
        if (Pattern.compile("<select\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("<label\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                if (!Pattern.compile("aria-label|aria-labelledby", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                    issues.add(AccessibilityIssue.builder()
                            .code("SELECT_LABEL_MISSING")
                            .message("select 요소에 label이나 aria-label이 없습니다.")
                            .startLine(0)
                            .endLine(0)
                            .snippet(code.substring(0, Math.min(400, code.length())))
                            .build());
                }
            }
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(75L);
    }
}
