package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 1.3.1 정보와 관계 (구조적 마크업)
 * 시각적 표현으로 전달되는 정보·구조·관계를 프로그래밍적으로 확인 가능하게 구현
 */
public class Sc131InformationRelationshipRule implements AccessibilityRule {

    private static final Pattern SEMANTIC_TAG_PATTERN = Pattern.compile("<(header|nav|main|article|section|aside|footer)\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADING_TAG_PATTERN = Pattern.compile("<h[1-6]\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIV_ONLY_LAYOUT = Pattern.compile("<div[^>]*>[^<]*<div[^>]*>[^<]*</div>\\s*</div>", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findSemanticIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.3.1")
                .name("정보와 관계 (구조적 마크업)")
                .level("A")
                .mvpDescription("시각적 표현으로 전달되는 정보, 구조, 관계를 프로그래밍적으로 확인 가능하게 구현합니다.")
                .implementationMethod("정적 분석 + AI")
                .implementationDescription("시맨틱 태그(header, nav, main, article 등) 사용 여부를 코드로 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .aiReviewRequired(true)
                .aiReviewGuide("구조의 의미적 적절성은 AI 판단이 필요합니다.")
                .defaultSuggestion("header, nav, main 등 시맨틱 태그와 계층적인 제목(h1~h6)을 사용해 구조를 명확히 하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findSemanticIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // 시맨틱 태그 사용 확인
        if (!Pattern.compile("<(header|nav|main|article|section|aside|footer)\\b", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("SEMANTIC_TAGS_MISSING")
                    .message("시맨틱 구조 태그(header, nav, main, article, section, aside, footer)가 없습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(200, code.length())))
                    .build());
        }

        // 제목 계층 구조 확인
        if (!Pattern.compile("<h[1-6]\\b", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            issues.add(AccessibilityIssue.builder()
                    .code("HEADING_HIERARCHY_MISSING")
                    .message("제목 태그(h1-h6)가 없습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(200, code.length())))
                    .build());
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(12L);
    }
}
