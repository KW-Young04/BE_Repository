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

    // 위반 여부 (CODE 타입 전용 — 코드만으로 확정 가능한 경우 사용.
    // CODE_AI/AI 타입은 이 필드 대신 각 IssueLocation.violated를 위치별로 사용한다.)
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

        // 이 위치가 실제로 위반인지 여부.
        // CODE_AI/AI 타입은 AI 응답을 파싱하면서 위치별로 채워짐 (result.violated 대신 이걸 기준으로 저장 여부 판단).
        // CODE 타입은 result.violated를 그대로 따르므로 이 필드는 null로 둬도 무방.
        private Boolean violated;
    }
}