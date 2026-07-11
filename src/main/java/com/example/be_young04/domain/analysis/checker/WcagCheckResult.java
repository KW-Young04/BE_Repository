package com.example.be_young04.domain.analysis.checker;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WcagCheckResult {

    // WCAG 항목 ID (예: "1.1.1") — SC, 참고/그룹핑용
    private String wcagId;

    // WCAG_ITEMS 고정 PK (1~87)
    private Long wcagItemId;

    // 판단 유형
    private JudgeType judgeType;

    // 위반 여부 (CODE만 확정 가능, CODE_AI/AI는 null로 두고 AI 최종 판단에 위임)
    private Boolean violated;

    // AI에게 넘길 추가 컨텍스트 (CODE_AI, AI인 경우)
    private String aiContext;

    // 체커 판단 결과 메시지
    private String message;

    // TARGET_FILE_PATH
    private String filePath;

    // WCAG_ITEMS.TITLE 참조용
    private String title;

    // 위반 위치들 (파일 내 여러 곳에서 발견될 수 있음 — 이슈 1개 : 위치 N개)
    @Builder.Default
    private List<IssueLocation> locations = List.of();

    public enum JudgeType {
        CODE,       // 코드만으로 판단 가능
        CODE_AI,    // 코드 분석 후 AI 추가 판단 필요
        AI          // AI만 판단 가능
    }

    @Getter
    @Builder
    public static class IssueLocation {
        private String cssSelector;
        private String violatedCode;
        private String suggestion;
    }
}