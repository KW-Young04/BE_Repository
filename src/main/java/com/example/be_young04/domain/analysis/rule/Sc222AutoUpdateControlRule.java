package com.example.be_young04.domain.analysis.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SC 2.2.2 일시정지/정지/숨기기 - 자동 업데이트
 * 자동 시작·병행 표시 자동 업데이트 콘텐츠에 일시정지·정지·숨기기·빈도 제어 제공
 */
public class Sc222AutoUpdateControlRule implements AccessibilityRule {

    @Override
    public AccessibilityCheckResult analyze(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        if (code != null && !code.isBlank()) {
            issues.addAll(findAutoUpdateControlIssues(code));
        }

        return AccessibilityCheckResult.builder()
                .successCriteria("2.2.2")
                .name("일시정지/정지/숨기기 - 자동 업데이트")
                .level("A")
                .mvpDescription("자동 시작 또는 자동 업데이트되는 콘텐츠에 일시정지·정지·숨기기 메커니즘을 제공합니다.")
                .implementationMethod("정적 분석")
                .implementationDescription("setInterval·WebSocket 자동 업데이트 패턴을 감지하고 제어 UI를 확인합니다.")
                .status(issues.isEmpty() ? "PASS" : "WARN")
                .issues(issues)
                .build();
    }

    private List<AccessibilityIssue> findAutoUpdateControlIssues(String code) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // setInterval 자동 업데이트 감지
        if (Pattern.compile("setInterval\\s*\\(", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("(pause|stop|disable|control|clearInterval)", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("AUTO_UPDATE_NO_CONTROL")
                        .message("setInterval로 자동 업데이트가 있지만 제어 메커니즘이 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        // WebSocket 실시간 업데이트 감지
        if (Pattern.compile("WebSocket|websocket|ws://|wss://", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
            if (!Pattern.compile("(pause|stop|disconnect)", Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                issues.add(AccessibilityIssue.builder()
                        .code("WEBSOCKET_NO_CONTROL")
                        .message("WebSocket 실시간 업데이트가 있지만 제어 메커니즘이 없습니다.")
                        .startLine(0)
                        .endLine(0)
                        .snippet(code.substring(0, Math.min(400, code.length())))
                        .build());
            }
        }

        return issues;
    }
}
