package com.example.be_young04.domain.analysis.checker;

import com.example.be_young04.domain.analysis.rule.AccessibilityRule;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.beans.factory.config.BeanDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * accessibility.rule 패키지(현 domain.analysis.rule)를 스캔해
 * AccessibilityRule 구현체를 전부 찾아 인스턴스화한다.
 * 룰은 상태 없는 순수 POJO(기본 생성자만)이므로 리플렉션으로 바로 생성 가능.
 */
public final class AccessibilityRuleScanner {

    private static final String RULE_PACKAGE = "com.example.be_young04.domain.analysis.rule";

    private AccessibilityRuleScanner() {
    }

    public static List<AccessibilityRule> scanAll() {
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(AccessibilityRule.class));

        List<AccessibilityRule> rules = new ArrayList<>();
        for (BeanDefinition bd : provider.findCandidateComponents(RULE_PACKAGE)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (clazz.isInterface()) continue; // AccessibilityRule 인터페이스 자체 제외
                AccessibilityRule rule = (AccessibilityRule) clazz.getDeclaredConstructor().newInstance();
                rules.add(rule);
            } catch (Exception e) {
                throw new IllegalStateException("룰 인스턴스화 실패: " + bd.getBeanClassName(), e);
            }
        }
        return rules;
    }
}