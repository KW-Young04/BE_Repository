package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.accessibility.rule.AccessibilityRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111ControlInputNameRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111DecorativeContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111NonTextContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc121AudioOnlyPrerecordedRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc121VideoOnlyPrerecordedRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc231FlashThresholdRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc241BypassBlocksRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc242PageTitleRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc243FocusOrderRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc311PageLanguageRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc321NoContextChangeOnFocusRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc322NoContextChangeOnInputRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc331ErrorIdentificationRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc332LabelsOrInstructionsRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc412NameRoleValueRule;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.List;

public class AccessibilityAnalyzer {

    private final List<AccessibilityRule> rules = List.of(
            new Sc111NonTextContentRule(),
            new Sc111ControlInputNameRule(),
            new Sc111DecorativeContentRule(),

            new Sc121AudioOnlyPrerecordedRule(),
            new Sc121VideoOnlyPrerecordedRule(),

            new Sc231FlashThresholdRule(),
            new Sc241BypassBlocksRule(),
            new Sc242PageTitleRule(),
            new Sc243FocusOrderRule(),

            new Sc311PageLanguageRule(),
            new Sc321NoContextChangeOnFocusRule(),
            new Sc322NoContextChangeOnInputRule(),
            new Sc331ErrorIdentificationRule(),
            new Sc332LabelsOrInstructionsRule(),

            new Sc412NameRoleValueRule()
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

    public List<AccessibilityCheckResult> analyzeSc231(String code) {
        return List.of(new Sc231FlashThresholdRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc241(String code) {
        return List.of(new Sc241BypassBlocksRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc242(String code) {
        return List.of(new Sc242PageTitleRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc243(String code) {
        return List.of(new Sc243FocusOrderRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc311(String code) {
        return List.of(new Sc311PageLanguageRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc321(String code) {
        return List.of(new Sc321NoContextChangeOnFocusRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc322(String code) {
        return List.of(new Sc322NoContextChangeOnInputRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc331(String code) {
        return List.of(new Sc331ErrorIdentificationRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc332(String code) {
        return List.of(new Sc332LabelsOrInstructionsRule().analyze(code));
    }

    public List<AccessibilityCheckResult> analyzeSc412(String code) {
        return List.of(new Sc412NameRoleValueRule().analyze(code));
    }

    public List<String> analyzeIssueMessages(String code) {
        return analyze(code).stream()
                .flatMap(result -> result.getIssues().stream())
                .map(AccessibilityIssue::getMessage)
                .toList();
    }
}