package com.example.be_young04.domain.analysis.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WcagCheckerRegistry {

    // Spring이 WcagChecker 구현체들을 자동으로 주입해줌
    private final List<WcagChecker> checkers;

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