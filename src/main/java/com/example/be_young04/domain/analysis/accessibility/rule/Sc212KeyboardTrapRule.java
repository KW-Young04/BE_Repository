package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 2.1.2 키보드 트랩 없음 - 비간섭 요건
 * 키보드 트랩 발생 시 전체 페이지 사용 방해
 */
public class Sc212KeyboardTrapRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findKeyboardTrapIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.1.2")
                .name("키보드 트랩 없음 - 비간섭 요건")
                .level("A")
                .mvpDescription("키보드 트랩이 없거나, 트랩에서 벗어날 수 있는 방법을 제공합니다.")
                .implementationMethod("코드 + AI 분석")
                .implementationDescription("tabindex 순환 패턴을 감지하고, 실제 트랩 여부는 AI로 판단합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .aiReviewRequired(true)
                .aiReviewGuide("실제 키보드 트랩 여부는 AI 분석이 필요합니다.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findKeyboardTrapIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // tabindex 사용 현황 확인
        if (Pattern.compile("tabindex\\s*=", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            // 순환 tabindex 패턴 감지 (예: tabindex="1" ... tabindex="0" 반복)
            if (Pattern.compile("tabindex\\s*=\\s*['\"]\\d+['\"].*?tabindex\\s*=\\s*['\"]0['\"].*?tabindex\\s*=\\s*['\"]\\d+['\"]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("TABINDEX_CIRCULATION")
                        .message("tabindex 속성이 순환 패턴으로 사용되고 있습니다. 키보드 네비게이션 테스트가 필요합니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        return issues;
    }
}
