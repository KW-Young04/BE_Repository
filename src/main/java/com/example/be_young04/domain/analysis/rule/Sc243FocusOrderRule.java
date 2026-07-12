package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 2.4.3 포커스 순서
 * 순차 탐색 시 의미·조작에 영향을 주는 포커스 순서가 논리적이어야 함
 */
public class Sc243FocusOrderRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findFocusOrderIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.4.3")
                .name("포커스 순서")
                .level("A")
                .mvpDescription("순차 탐색 시 포커스 순서가 논리적이고 의미가 있습니다.")
                .implementationMethod("코드 + AI 분석")
                .implementationDescription("tabindex 값을 분석하고, 논리적 순서의 적절성은 AI로 판단합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .aiReviewRequired(true)
                .aiReviewGuide("포커스 순서의 논리적 적절성은 AI 분석이 필요합니다.")
                .defaultSuggestion("양수 tabindex 사용을 피하고, DOM 순서만으로 논리적인 포커스 흐름이 되도록 하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findFocusOrderIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // 0이 아닌 양수 tabindex 사용 감지 (비권장)
        if (Pattern.compile("tabindex\\s*=\\s*['\"]([1-9]\\d*)['\"]", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("TABINDEX_POSITIVE_VALUES")
                    .message("양수 tabindex 값이 사용되었습니다. 0 또는 -1만 권장됩니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        // tabindex=-1 과도 사용 감지
        int tabindexNegativeCount = (int) Pattern.compile("tabindex\\s*=\\s*['\"]\\-1['\"]", Pattern.CASE_INSENSITIVE).matcher(code).results().count();
        if (tabindexNegativeCount > 5) {
            issues.add(AccessibilityIssue.builder()
                    .code("EXCESSIVE_TABINDEX_NEGATIVE")
                    .message("tabindex='-1'이 과도하게 사용되었습니다. 포커스 순서 검토가 필요합니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(400, code.length())))
                    .build());
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(52L);
    }
}
