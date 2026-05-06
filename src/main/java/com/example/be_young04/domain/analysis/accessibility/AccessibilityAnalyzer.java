package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.accessibility.rule.AccessibilityRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111ControlInputNameRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111DecorativeContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111NonTextContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc121AudioOnlyPrerecordedRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc121VideoOnlyPrerecordedRule;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.List;

public class AccessibilityAnalyzer {

    private final List<AccessibilityRule> rules = List.of(
            new Sc111NonTextContentRule(),
            new Sc111ControlInputNameRule(),
            new Sc121AudioOnlyPrerecordedRule(),
            new Sc121VideoOnlyPrerecordedRule()
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

    public List<AccessibilityCheckResult> analyzeSc111ControlInput(String code) {
        return List.of(new Sc111ControlInputNameRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc121(String code) {
        return List.of(new Sc121AudioOnlyPrerecordedRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc121VideoOnly(String code) {
        return List.of(new Sc121VideoOnlyPrerecordedRule().analyze(code));
    }

    public List<String> analyzeIssueMessages(String code) {
        return analyze(code).stream()
                .flatMap(result -> result.getIssues().stream())
                .map(AccessibilityIssue::getMessage)
                .toList();
    }
}
