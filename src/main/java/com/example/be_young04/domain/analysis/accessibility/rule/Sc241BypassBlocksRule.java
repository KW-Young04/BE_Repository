package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Sc241BypassBlocksRule implements AccessibilityRule {

    private static final Pattern SKIP_LINK_PATTERN =
            Pattern.compile("<a\\b[^>]*href\\s*=\\s*[\"']#(?:main|content|main-content|본문|skip)[\"'][^>]*>.*?</a>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern MAIN_LANDMARK_PATTERN =
            Pattern.compile("<main\\b|role\\s*=\\s*[\"']main[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern NAV_LANDMARK_PATTERN =
            Pattern.compile("<nav\\b|role\\s*=\\s*[\"']navigation[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEADER_PATTERN =
            Pattern.compile("<header\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        boolean hasSkipLink = false;
        boolean hasMainLandmark = false;
        boolean hasNavLandmark = false;
        boolean hasHeader = false;

        if (code != null && !code.isBlank()) {
            hasSkipLink = SKIP_LINK_PATTERN.matcher(code).find();
            hasMainLandmark = MAIN_LANDMARK_PATTERN.matcher(code).find();
            hasNavLandmark = NAV_LANDMARK_PATTERN.matcher(code).find();
            hasHeader = HEADER_PATTERN.matcher(code).find();
        }

        if (!hasSkipLink && !hasMainLandmark) {
            issues.add(buildIssue(
                    "BYPASS_BLOCK_MISSING",
                    "반복 콘텐츠를 건너뛰기 위한 skip-nav 링크 또는 main landmark가 없습니다.",
                    code,
                    0,
                    "skip-nav 또는 main landmark 미검출"
            ));
        }

        boolean aiReviewRequired = issues.isEmpty() && (hasNavLandmark || hasHeader);

        return AccessibilityCheckResult.builder()
                .successCriteria("2.4.1")
                .name("블록 건너뛰기")
                .level("A")
                .mvpDescription("반복되는 콘텐츠 블록을 건너뛸 수 있는 메커니즘을 제공해야 합니다.")
                .implementationMethod("코드")
                .implementationDescription("skip-nav 링크 또는 main/nav/header 등 landmark 존재 여부를 코드로 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .aiReviewRequired(aiReviewRequired)
                .aiReviewGuide(aiReviewRequired ? "landmark 구조가 실제로 키보드 사용자의 주요 콘텐츠 이동을 돕는지 검토할 수 있습니다." : null)
                .issues(issues)
                .build();
    }

    private AccessibilityIssue buildIssue(String type, String message, String code, int startIndex, String snippet) {
        return AccessibilityIssue.builder()
                .type(type)
                .message(message)
                .line(calculateLineNumber(code, startIndex))
                .snippet(snippet)
                .build();
    }

    private int calculateLineNumber(String code, int startIndex) {
        if (code == null) {
            return 1;
        }

        int line = 1;
        for (int i = 0; i < startIndex && i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}