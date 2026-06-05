package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.accessibility.rule.*;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.List;

public class AccessibilityAnalyzer {

    private final List<AccessibilityRule> rules = List.of(
            // 1.1.1 이미지 대체 텍스트
            new Sc111NonTextContentRule(),
            new Sc111DecorativeContentRule(),
            new Sc111ControlInputNameRule(),
            // 1.2.1-1.2.3 미디어
            new Sc121AudioOnlyPrerecordedRule(),
            new Sc121VideoOnlyPrerecordedRule(),
            new Sc122CaptionsPrerecordedRule(),
            new Sc123AudioDescriptionRule(),
            // 1.3.1 정보와 관계
            new Sc131InformationRelationshipRule(),
            // 1.4.1-1.4.2 색상 및 오디오
            new Sc141ColorOnlyRule(),
            new Sc142AudioControlRule(),
            // 2.1.2 키보드 트랩
            new Sc212KeyboardTrapRule(),
            // 2.2.1-2.2.2 타이밍
            new Sc221TimingAdjustableRule(),
            new Sc222AutoUpdateControlRule(),
            // 2.3.1 점멸
            new Sc231BlinkingRule(),
            // 2.4.1-2.4.3 네비게이션
            new Sc241SkipBlockRule(),
            new Sc242PageTitleRule(),
            new Sc243FocusOrderRule(),
            // 3.1.1 페이지 언어
            new Sc311PageLanguageRule(),
            // 3.2.1-3.2.2 예측 가능성
            new Sc321FocusChangeRule(),
            new Sc322InputChangeRule(),
            // 3.3.1-3.3.2 입력 보조
            new Sc331ErrorIdentificationRule(),
            new Sc332LabelDescriptionRule(),
            // 4.1.2 이름·역할·값
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

    public List<String> analyzeIssueMessages(String code) {
        return analyze(code).stream()
                .flatMap(result -> result.getIssues().stream())
                .map(AccessibilityIssue::getMessage)
                .toList();
    }
}
