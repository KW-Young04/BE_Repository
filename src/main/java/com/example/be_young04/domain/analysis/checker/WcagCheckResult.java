package com.example.be_young04.domain.analysis.checker;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WcagCheckResult {

    // WCAG 항목 ID (예: "1.1.1")
    private String wcagId;

    // 판단 유형
    private JudgeType judgeType;

    // 위반 여부 (코드로 판단 가능한 경우만)
    private Boolean violated;

    // 위반된 코드 위치 및 내용
    private String violatedCode;

    // AI에게 넘길 추가 컨텍스트 (코드+AI, AI 전용인 경우)
    private String aiContext;

    // 체커 판단 결과 메시지
    private String message;

    public enum JudgeType {
        CODE,       // 코드만으로 판단 가능
        CODE_AI,    // 코드 분석 후 AI 추가 판단 필요
        AI          // AI만 판단 가능
    }
}