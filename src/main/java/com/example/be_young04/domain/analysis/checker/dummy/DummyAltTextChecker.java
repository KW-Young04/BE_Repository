package com.example.be_young04.domain.analysis.checker.dummy;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [DUMMY] 실제 구현 전 테스트용
 * WCAG 1.1.1 — 이미지 대체 텍스트(alt) 확인
*/

@Component
public class DummyAltTextChecker implements WcagChecker {

    @Override
    public String getWcagId() {
        return "1.1.1";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("html", "tsx", "jsx");
    }

    @Override
    public WcagCheckResult check(String fileName, String fileContent) {
        // [DUMMY] 항상 위반으로 판정하는 가짜 로직
        boolean hasImgWithoutAlt = fileContent.contains("<img") && !fileContent.contains("alt=");

        return WcagCheckResult.builder()
                .wcagId(getWcagId())
                .judgeType(WcagCheckResult.JudgeType.CODE)
                .violated(hasImgWithoutAlt)
                .violatedCode(hasImgWithoutAlt ? "<img src=\"...\" /> (alt 없음)" : null)
                .message(hasImgWithoutAlt
                        ? "[DUMMY] alt 속성이 없는 img 태그 발견"
                        : "[DUMMY] alt 속성 정상 확인")
                .build();
    }
}