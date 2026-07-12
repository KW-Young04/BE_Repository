package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 1.4.1 색상 사용 - 색상 인식 전용
 * 정보 전달·행동 유도·구분에 색상만 사용 금지
 */
public class Sc141ColorOnlyRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findColorOnlyIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.4.1")
                .name("색상 사용 - 색상 인식 전용")
                .level("A")
                .mvpDescription("정보 전달, 행동 유도, 구분에 색상만 사용하지 않습니다.")
                .implementationMethod("코드 + AI 혼합 분석")
                .implementationDescription("CSS color 단독 사용 패턴을 감지하고, 실제 정보 전달 여부는 AI로 판단합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .aiReviewRequired(true)
                .aiReviewGuide("색상만으로 정보를 전달하는지는 AI 분석이 필요합니다.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findColorOnlyIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // 색상만으로 정보 표현하는 패턴 감지
        if (Pattern.compile("color\\s*:\\s*[^;]+;|background-color\\s*:\\s*[^;]+;", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("(icon|aria-label|title|alt|[>].*?[<]|::before|::after)", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("COLOR_ONLY_USAGE")
                        .message("CSS에서 색상 속성이 있지만 텍스트, 아이콘, ARIA 레이블 등의 대체 수단이 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(300, code.length())))
                        .build());
            }
        }

        return issues;
    }
}
