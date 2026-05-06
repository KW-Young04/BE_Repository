package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.accessibility.rule.AccessibilityRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111DecorativeContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111NonTextContentRule;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.List;

public class AccessibilityAnalyzer {

    private final List<AccessibilityRule> rules = List.of(
            new Sc111NonTextContentRule()
    );

    public List<AccessibilityCheckResult> analyze(String code) {
        return rules.stream()
                .map(rule -> rule.analyze(code))
                .toList();
    }

    public List<AccessibilityCheckResult> analyzeSc111(String code) {
        return List.of(new Sc111NonTextContentRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc111Decorative(String code) {
        return List.of(new Sc111DecorativeContentRule().analyze(code));
    }

    public List<String> analyzeIssueMessages(String code) {
        return analyze(code).stream()
                .flatMap(result -> result.getIssues().stream())
                .map(AccessibilityIssue::getMessage)
                .toList();
    }
}
