package com.example.be_young04.domain.analysis.checker;

import com.example.be_young04.domain.analysis.accessibility.rule.AccessibilityRule;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.IssueLocation;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.List;

/**
 * AccessibilityRule(정적 분석 룰) 구현체를 WcagChecker로 연결하는 어댑터.
 * Rule 1개 + wcagItemId 1개 = WcagChecker 인스턴스 1개.
 * (하나의 Rule이 여러 WCAG_ITEM_ID에 대응하는 경우 - 예: 2.2.1 -
 *  같은 Rule로 어댑터 인스턴스를 여러 개 만들어 각각 다른 wcagItemId를 부여한다.)
 */
public class AccessibilityRuleWcagChecker implements WcagChecker {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of("html", "tsx", "jsx");

    private final AccessibilityRule rule;
    private final Long wcagItemId;
    private final String wcagId;
    private final String title;

    public AccessibilityRuleWcagChecker(AccessibilityRule rule, Long wcagItemId) {
        this.rule = rule;
        this.wcagItemId = wcagItemId;

        // successCriteria/name은 입력과 무관한 고정값이라 빈 문자열로 1회 호출해 캐싱
        AccessibilityCheckResult probe = rule.analyze("");
        this.wcagId = probe.getSuccessCriteria();
        this.title = probe.getName();
    }

    @Override
    public String getWcagId() {
        return wcagId;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public WcagCheckResult check(String fileName, String fileContent) {
        AccessibilityCheckResult result = rule.analyze(fileContent);

        boolean aiReviewRequired = result.isAiReviewRequired();
        JudgeType judgeType = aiReviewRequired ? JudgeType.CODE_AI : JudgeType.CODE;
        Boolean violated = aiReviewRequired ? null : "FAIL".equals(result.getStatus());

        return WcagCheckResult.builder()
                .wcagId(wcagId)
                .wcagItemId(wcagItemId)
                .title(title)
                .judgeType(judgeType)
                .violated(violated)
                .message(aiReviewRequired ? result.getAiReviewGuide() : result.getMvpDescription())
                .aiContext(aiReviewRequired ? result.getImplementationDescription() : null)
                .filePath(fileName)
                .locations(toLocations(result.getIssues()))
                .build();
    }

    private List<IssueLocation> toLocations(List<AccessibilityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return issues.stream()
                .map(issue -> IssueLocation.builder()
                        .cssSelector(null) // AccessibilityIssue엔 selector 정보 없음 (line/snippet만 존재)
                        .violatedCode(issue.getSnippet())
                        .suggestion(issue.getMessage())
                        .build())
                .toList();
    }
}