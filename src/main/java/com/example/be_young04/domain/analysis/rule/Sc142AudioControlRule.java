package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 1.4.2 오디오 제어 - 비간섭 요건
 * 3초 초과 자동 재생 오디오에 일시정지·정지 또는 음량 독립 조절 메커니즘 제공
 */
public class Sc142AudioControlRule implements AccessibilityRule {

    private static final Pattern AUTOPLAY_PATTERN = Pattern.compile("<(audio|video)\\b[^>]*autoplay", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUDIO_TAG_PATTERN = Pattern.compile("<audio\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findAudioControlIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.4.2")
                .name("오디오 제어 - 비간섭 요건")
                .level("A")
                .mvpDescription("3초 초과 자동 재생 오디오에 일시정지·정지 또는 음량 독립 조절 메커니즘을 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("autoplay 속성 감지 및 볼륨/정지 컨트롤 UI 존재 여부를 검사합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("자동 재생 오디오/비디오에 controls 속성을 추가해 정지·음소거가 가능하게 하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findAudioControlIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // autoplay 속성이 있는 audio/video 태그 감지
        if (AUTOPLAY_PATTERN.matcher(code).find()) {
            // controls 속성이 있는지 확인
            if (!Pattern.compile("<(audio|video)\\b[^>]*controls", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("AUTOPLAY_NO_CONTROL")
                        .message("autoplay 속성이 있지만 controls 속성이 없습니다. 사용자가 재생을 제어할 수 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(300, code.length())))
                        .build());
            }
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(32L);
    }
}
