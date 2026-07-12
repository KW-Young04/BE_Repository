package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SC 2.4.1 블록 건너뛰기
 * 반복 콘텐츠 블록을 건너뛸 메커니즘 제공
 */
public class Sc241SkipBlockRule implements AccessibilityRule {

    private static final Pattern SKIP_LINK_PATTERN = Pattern.compile("<a\\b[^>]*href\\s*=\\s*['\"]#['\"][^>]*>.*?skip.*?</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LANDMARK_PATTERN = Pattern.compile("role\\s*=\\s*['\"]?(main|navigation|contentinfo)['\"]?", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findSkipBlockIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.4.1")
                .name("블록 건너뛰기")
                .level("A")
                .mvpDescription("반복 콘텐츠 블록을 건너뛸 수 있는 메커니즘을 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("skip-nav 링크 또는 ARIA landmark(main, nav 등) 존재 여부를 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findSkipBlockIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // skip 링크 확인
        boolean hasSkipLink = SKIP_LINK_PATTERN.matcher(code).find();
        
        // landmark 확인
        boolean hasLandmark = Pattern.compile("<(header|nav|main|article|section|aside|footer)\\b", Pattern.CASE_INSENSITIVE).matcher(code).find() ||
                             LANDMARK_PATTERN.matcher(code).find();

        if (!hasSkipLink && !hasLandmark) {
            issues.add(AccessibilityIssue.builder()
                    .code("SKIP_BLOCK_MISSING")
                    .message("skip 링크나 ARIA landmark가 없습니다. 반복 콘텐츠를 건너뛸 방법이 없습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(300, code.length())))
                    .build());
        }

        return issues;
    }
}
