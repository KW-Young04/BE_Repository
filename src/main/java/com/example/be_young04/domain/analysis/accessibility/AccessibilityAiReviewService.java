package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.service.AiAnalysisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessibilityAiReviewService {

    private final AiAnalysisClient aiAnalysisClient;

    public AccessibilityCheckResult reviewControlInputName(String code, AccessibilityCheckResult staticResult) {
        if (!staticResult.isAiReviewRequired()) {
            return staticResult;
        }

        try {
            String aiResult = aiAnalysisClient.analyze(buildControlInputNamePrompt(code), null);
            String aiStatus = extractAiStatus(aiResult);

            return copyWithAiReview(
                    staticResult,
                    mapFinalStatus(aiStatus),
                    aiStatus,
                    aiResult
            );
        } catch (Exception e) {
            return copyWithAiReview(
                    staticResult,
                    "AI_REVIEW_FAILED",
                    "ERROR",
                    "AI 검토 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    private String buildControlInputNamePrompt(String code) {
        String safeCode = code == null ? "" : code;
        if (safeCode.length() > 5000) {
            safeCode = safeCode.substring(0, 5000);
        }

        return """
                당신은 웹 접근성 검사자입니다.

                검사 기준:
                - WCAG SC 1.1.1 이미지 대체 텍스트 - 컨트롤/입력
                - 입력을 받거나 컨트롤 역할을 하는 비텍스트 콘텐츠에는 목적을 설명하는 accessible name이 있어야 합니다.

                코드 정적 분석에서는 aria-label, aria-labelledby, title, alt 같은 name 후보가 존재하는 것까지만 확인했습니다.
                이제 아래 코드에서 해당 name이 컨트롤의 실제 목적을 충분히 설명하는지 판단하세요.

                반드시 첫 줄은 아래 셋 중 하나로만 시작하세요.
                PASS: name이 목적을 충분히 설명함
                WARN: 일부 name이 모호하거나 개선 여지가 있음
                FAIL: name이 목적을 설명하지 못함

                그 다음 줄부터 근거와 개선안을 한국어로 짧게 작성하세요.

                [검사 코드]
                ```jsx
                %s
                ```
                """.formatted(safeCode);
    }

    private String extractAiStatus(String aiResult) {
        if (aiResult == null || aiResult.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = aiResult.stripLeading().toUpperCase();
        if (normalized.startsWith("PASS")) {
            return "PASS";
        }
        if (normalized.startsWith("WARN")) {
            return "WARN";
        }
        if (normalized.startsWith("FAIL")) {
            return "FAIL";
        }
        return "UNKNOWN";
    }

    private String mapFinalStatus(String aiStatus) {
        return switch (aiStatus) {
            case "PASS" -> "PASS";
            case "WARN" -> "WARN";
            case "FAIL" -> "FAIL";
            default -> "AI_REVIEWED";
        };
    }

    private AccessibilityCheckResult copyWithAiReview(
            AccessibilityCheckResult source,
            String status,
            String aiReviewStatus,
            String aiReviewResult
    ) {
        return AccessibilityCheckResult.builder()
                .successCriteria(source.getSuccessCriteria())
                .name(source.getName())
                .level(source.getLevel())
                .mvpDescription(source.getMvpDescription())
                .implementationMethod(source.getImplementationMethod())
                .implementationDescription(source.getImplementationDescription())
                .status(status)
                .aiReviewRequired(false)
                .aiReviewGuide(source.getAiReviewGuide())
                .aiReviewStatus(aiReviewStatus)
                .aiReviewResult(aiReviewResult)
                .issues(source.getIssues())
                .build();
    }
}
