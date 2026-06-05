package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SC 1.2.2 자막 제공 (사전녹음)
 * 동기화 미디어 내 사전녹음 오디오에 자막 제공
 */
public class Sc122CaptionsPrerecordedRule implements AccessibilityRule {

    private static final Pattern VIDEO_TAG_PATTERN = Pattern.compile("<video\\b[^>]*>(.*?)</video>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TRACK_CAPTIONS_PATTERN = Pattern.compile("<track\\b[^>]*kind\\s*=\\s*['\"]captions['\"][^>]*>", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findCaptionsIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("1.2.2")
                .name("자막 제공 (사전녹음)")
                .level("A")
                .mvpDescription("동기화 미디어 내 사전녹음 오디오에 자막을 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("video 태그 내 track[kind=captions] 존재 여부를 검사합니다.")
                .status(issues.isEmpty() ? "PASS" : "FAIL")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findCaptionsIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        Matcher videoMatcher = VIDEO_TAG_PATTERN.matcher(code);

        while (videoMatcher.find()) {
            String videoTag = videoMatcher.group();
            String videoContent = videoMatcher.group(1);

            if (!TRACK_CAPTIONS_PATTERN.matcher(videoContent).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("VIDEO_CAPTIONS_MISSING")
                        .message("video 태그 내 kind='captions' 속성을 가진 track 태그가 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(videoTag)
                        .build());
            }
        }

        return issues;
    }
}
