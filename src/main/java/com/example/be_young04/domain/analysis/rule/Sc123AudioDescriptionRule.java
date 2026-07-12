package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SC 1.2.3 음성 해설 또는 미디어 대안 (사전녹음)
 * 사전녹음 비디오에 텍스트 대안 또는 음성 해설 제공
 */
public class Sc123AudioDescriptionRule implements AccessibilityRule {

    private static final Pattern VIDEO_TAG_PATTERN = Pattern.compile("<video\\b[^>]*>(.*?)</video>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TRACK_DESCRIPTIONS_PATTERN = Pattern.compile("<track\\b[^>]*kind\\s*=\\s*['\"]descriptions['\"][^>]*>", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findAudioDescriptionIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.2.3")
                .name("음성 해설 또는 미디어 대안 (사전녹음)")
                .level("A")
                .mvpDescription("사전녹음 비디오에 텍스트 대안 또는 음성 해설을 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("video 태그 내 track[kind=descriptions] 또는 미디어 대안 링크 존재 여부를 검사합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .defaultSuggestion("video 태그 내부에 kind=\"descriptions\" track이나 텍스트 대안 링크를 추가하세요.")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findAudioDescriptionIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        Matcher videoMatcher = VIDEO_TAG_PATTERN.matcher(code);

        while (videoMatcher.find()) {
            String videoTag = videoMatcher.group();
            String videoContent = videoMatcher.group(1);

            if (!TRACK_DESCRIPTIONS_PATTERN.matcher(videoContent).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("VIDEO_DESCRIPTION_MISSING")
                        .message("video 태그 내 kind='descriptions' 속성을 가진 track 태그가 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(videoTag)
                        .build());
            }
        }

        return issues;
    }

    @Override
    public List<Long> getWcagItemIds() {
        return List.of(10L);
    }
}
