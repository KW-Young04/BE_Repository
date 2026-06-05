package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SC 2.2.1 타이밍 조정 (조정/끄기/예외)
 * 시간 제한을 기본값의 10배 이상 범위로 조정 가능하거나 끌 수 있어야 함
 */
public class Sc221TimingAdjustableRule implements AccessibilityRule {

    private static final Pattern SETTIMEOUT_PATTERN = Pattern.compile("setTimeout\\s*\\(\\s*(?:function|\\(.*?\\)\\s*=>)\\s*\\{[^}]*\\}\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SETINTERVAL_PATTERN = Pattern.compile("setInterval\\s*\\(\\s*(?:function|\\(.*?\\)\\s*=>)\\s*\\{[^}]*\\}\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findTimingIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.2.1")
                .name("타이밍 조정 (조정/끄기/예외)")
                .level("A")
                .mvpDescription("시간 제한을 조정하거나 끌 수 있으며, 예외 사항을 명확히 합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("setTimeout/setInterval 감지 후 값을 계산하고, 타이머 제어 UI 존재 여부를 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findTimingIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // setTimeout 분석
        Matcher setTimeoutMatcher = SETTIMEOUT_PATTERN.matcher(code);
        while (setTimeoutMatcher.find()) {
            try {
                long timeout = Long.parseLong(setTimeoutMatcher.group(1));
                // 20시간 이상은 예외
                long twentyHours = 20 * 60 * 60 * 1000;
                if (timeout < twentyHours && !hasTimerControl(code)) {
                    issues.add(AccessibilityIssue.builder()
                            .code("TIMING_NO_CONTROL")
                            .message("setTimeout으로 시간 제한이 설정되어 있지만 사용자가 제어할 수 없습니다.")
                            .startLine(0)
                            .endLine(0)
                            .snippet(setTimeoutMatcher.group())
                            .build());
                }
            } catch (NumberFormatException e) {
                // 무시
            }
        }

        // setInterval 분석 (자동 업데이트)
        if (SETINTERVAL_PATTERN.matcher(code).find() && !hasUpdateControl(code)) {
            issues.add(AccessibilityIssue.builder()
                    .code("AUTO_UPDATE_NO_CONTROL")
                    .message("setInterval로 자동 업데이트가 설정되어 있지만 일시정지/정지 컨트롤이 없습니다.")
                    .startLine(0)
                    .endLine(0)
                    .snippet(code.substring(0, Math.min(300, code.length())))
                    .build());
        }

        return issues;
    }

    private boolean hasTimerControl(String code) {
        // 타이머 조정 또는 비활성화 UI 확인
        return Pattern.compile("(extendSession|resetTimer|adjustTimer|disable.*timer)", Pattern.CASE_INSENSITIVE).matcher(code).find();
    }

    private boolean hasUpdateControl(String code) {
        // 자동 업데이트 제어 UI 확인
        return Pattern.compile("(pause|stop|disable|control)", Pattern.CASE_INSENSITIVE).matcher(code).find();
    }
}
