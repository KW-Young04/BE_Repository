package com.example.be_young04.domain.analysis.checker;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WcagCheckerRegistry {

    private final List<WcagChecker> checkers;

    // Spring이 주입하는 WcagChecker 빈 목록 + AccessibilityRule 어댑터 목록을 합쳐서 관리
    public WcagCheckerRegistry(List<WcagChecker> checkers) {
        this.checkers = new ArrayList<>(checkers);
        this.checkers.addAll(AccessibilityRuleCheckerFactory.createAll());
    }

    // 특정 파일에 적용 가능한 체커 목록 반환
    public List<WcagChecker> getCheckersFor(String fileName) {
        String extension = extractExtension(fileName);
        return checkers.stream()
                .filter(checker -> checker.getSupportedExtensions().contains(extension))
                .toList();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}