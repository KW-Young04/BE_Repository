package com.example.be_young04.domain.analysis.checker;

import com.example.be_young04.domain.analysis.rule.AccessibilityRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AccessibilityRuleCheckerFactory {

    private AccessibilityRuleCheckerFactory() {
    }

    public static List<WcagChecker> createAll() {
        List<WcagChecker> checkers = new ArrayList<>();
        Set<Long> seenWcagItemIds = new HashSet<>();

        for (AccessibilityRule rule : AccessibilityRuleScanner.scanAll()) {
            List<Long> wcagItemIds = rule.getWcagItemIds();
            if (wcagItemIds == null || wcagItemIds.isEmpty()) {
                throw new IllegalStateException(
                        rule.getClass().getSimpleName() + "의 getWcagItemIds()가 비어 있습니다.");
            }

            for (Long wcagItemId : wcagItemIds) {
                if (!seenWcagItemIds.add(wcagItemId)) {
                    throw new IllegalStateException(
                            "WCAG_ITEM_ID 중복 매핑 발견: " + wcagItemId
                                    + " (" + rule.getClass().getSimpleName() + ")");
                }
                checkers.add(new AccessibilityRuleWcagChecker(rule, wcagItemId));
            }
        }
        return checkers;
    }
}