package com.example.be_young04.domain.analysis.checker;

import com.example.be_young04.domain.analysis.accessibility.rule.Sc111ControlInputNameRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111DecorativeContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc111NonTextContentRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc121AudioOnlyPrerecordedRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc121VideoOnlyPrerecordedRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc122CaptionsPrerecordedRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc123AudioDescriptionRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc131InformationRelationshipRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc141ColorOnlyRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc142AudioControlRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc212KeyboardTrapRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc221TimingAdjustableRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc222AutoUpdateControlRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc231BlinkingRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc241SkipBlockRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc242PageTitleRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc243FocusOrderRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc311PageLanguageRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc321FocusChangeRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc322InputChangeRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc331ErrorIdentificationRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc332LabelDescriptionRule;
import com.example.be_young04.domain.analysis.accessibility.rule.Sc412NameRoleValueRule;

import java.util.List;

/**
 * accessibility.rule 패키지의 정적 분석 룰(AccessibilityRule) 구현체들을
 * WcagChecker로 변환하는 팩토리.
 * Rule들은 상태 없는 순수 POJO(의존성 없음)라 Spring Bean으로 등록하지 않고 직접 생성한다.
 * WCAG_ITEM_ID는 wcag.sql의 고정 PK(1~87) 기준.
 */
public final class AccessibilityRuleCheckerFactory {

    private AccessibilityRuleCheckerFactory() {
    }

    public static List<WcagChecker> createAll() {
        return List.of(
                new AccessibilityRuleWcagChecker(new Sc111NonTextContentRule(), 1L),
                new AccessibilityRuleWcagChecker(new Sc111DecorativeContentRule(), 2L),
                new AccessibilityRuleWcagChecker(new Sc111ControlInputNameRule(), 3L),
                new AccessibilityRuleWcagChecker(new Sc121AudioOnlyPrerecordedRule(), 7L),
                new AccessibilityRuleWcagChecker(new Sc121VideoOnlyPrerecordedRule(), 8L),
                new AccessibilityRuleWcagChecker(new Sc122CaptionsPrerecordedRule(), 9L),
                new AccessibilityRuleWcagChecker(new Sc123AudioDescriptionRule(), 10L),
                new AccessibilityRuleWcagChecker(new Sc131InformationRelationshipRule(), 12L),
                new AccessibilityRuleWcagChecker(new Sc141ColorOnlyRule(), 18L),
                new AccessibilityRuleWcagChecker(new Sc142AudioControlRule(), 32L),
                new AccessibilityRuleWcagChecker(new Sc212KeyboardTrapRule(), 36L),
                // 2.2.1 — 조정(41)과 끄기(42)를 함께 검사하는 로직이라 동일 Rule로 어댑터 2개 생성
                new AccessibilityRuleWcagChecker(new Sc221TimingAdjustableRule(), 41L),
                new AccessibilityRuleWcagChecker(new Sc221TimingAdjustableRule(), 42L),
                new AccessibilityRuleWcagChecker(new Sc222AutoUpdateControlRule(), 46L),
                new AccessibilityRuleWcagChecker(new Sc231BlinkingRule(), 48L),
                new AccessibilityRuleWcagChecker(new Sc241SkipBlockRule(), 49L),
                new AccessibilityRuleWcagChecker(new Sc242PageTitleRule(), 51L),
                new AccessibilityRuleWcagChecker(new Sc243FocusOrderRule(), 52L),
                new AccessibilityRuleWcagChecker(new Sc311PageLanguageRule(), 64L),
                new AccessibilityRuleWcagChecker(new Sc321FocusChangeRule(), 66L),
                new AccessibilityRuleWcagChecker(new Sc322InputChangeRule(), 67L),
                new AccessibilityRuleWcagChecker(new Sc331ErrorIdentificationRule(), 74L),
                new AccessibilityRuleWcagChecker(new Sc332LabelDescriptionRule(), 75L),
                new AccessibilityRuleWcagChecker(new Sc412NameRoleValueRule(), 86L)
        );
    }
}