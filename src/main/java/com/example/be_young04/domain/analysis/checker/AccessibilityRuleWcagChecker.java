package com.example.be_young04.domain.analysis.checker;

import com.example.be_young04.domain.analysis.rule.AccessibilityRule;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.IssueLocation;
import com.example.be_young04.domain.analysis.checker.WcagCheckResult.JudgeType;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.List;
import java.util.Set;

public class AccessibilityRuleWcagChecker implements WcagChecker {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of("html", "tsx", "jsx");

    private static final Set<String> ROOT_DOCUMENT_FILE_NAMES = Set.of(
            "index.html", "app.tsx", "app.jsx", "main.tsx", "main.jsx"
    );

    private final AccessibilityRule rule;
    private final Long wcagItemId;
    private final String wcagId;
    private final String title;

    public AccessibilityRuleWcagChecker(AccessibilityRule rule, Long wcagItemId) {
        this.rule = rule;
        this.wcagItemId = wcagItemId;

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
        if (rule.isRootDocumentOnly() && !isRootDocumentFile(fileName)) {
            return null;
        }

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


    private boolean isRootDocumentFile(String fileName) {
        if (fileName == null) return false;
        String baseName = fileName.contains("/")
                ? fileName.substring(fileName.lastIndexOf("/") + 1)
                : fileName;
        return ROOT_DOCUMENT_FILE_NAMES.contains(baseName.toLowerCase());
    }

    private List<IssueLocation> toLocations(List<AccessibilityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return issues.stream()
                .map(issue -> IssueLocation.builder()
                        .cssSelector(null)
                        .violatedCode(issue.getSnippet())
                        .suggestion(issue.getMessage())
                        .build())
                .toList();
    }
}