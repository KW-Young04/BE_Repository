package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.service.AiAnalysisClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessibilityAiReviewServiceTest {

    @Test
    void reviewControlInputNameMapsPassResult() {
        AccessibilityAiReviewService service = new AccessibilityAiReviewService(
                prompt -> "PASS: accessible name이 버튼 목적을 설명합니다."
        );

        AccessibilityCheckResult result = service.reviewControlInputName(
                "<button aria-label=\"검색\"><img src=\"/search.png\" /></button>",
                aiReviewRequiredResult()
        );

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getAiReviewStatus()).isEqualTo("PASS");
        assertThat(result.isAiReviewRequired()).isFalse();
        assertThat(result.getAiReviewResult()).contains("accessible name");
    }

    @Test
    void reviewControlInputNameMapsFailResult() {
        AccessibilityAiReviewService service = new AccessibilityAiReviewService(
                prompt -> "FAIL: name이 너무 모호합니다."
        );

        AccessibilityCheckResult result = service.reviewControlInputName(
                "<button aria-label=\"아이콘\"><img src=\"/search.png\" /></button>",
                aiReviewRequiredResult()
        );

        assertThat(result.getStatus()).isEqualTo("FAIL");
        assertThat(result.getAiReviewStatus()).isEqualTo("FAIL");
    }

    @Test
    void reviewControlInputNameKeepsStaticResultWhenAiReviewIsNotRequired() {
        AccessibilityAiReviewService service = new AccessibilityAiReviewService(new FailingAiClient());
        AccessibilityCheckResult staticResult = AccessibilityCheckResult.builder()
                .successCriteria("1.1.1")
                .name("test")
                .status("FAIL")
                .aiReviewRequired(false)
                .issues(List.of())
                .build();

        AccessibilityCheckResult result = service.reviewControlInputName("", staticResult);

        assertThat(result).isSameAs(staticResult);
    }

    private AccessibilityCheckResult aiReviewRequiredResult() {
        return AccessibilityCheckResult.builder()
                .successCriteria("1.1.1")
                .name("이미지 대체 텍스트 - 컨트롤/입력")
                .level("A")
                .status("NEEDS_AI_REVIEW")
                .aiReviewRequired(true)
                .aiReviewGuide("AI 검토 필요")
                .issues(List.of())
                .build();
    }

    private static class FailingAiClient implements AiAnalysisClient {
        @Override
        public String analyze(String prompt) {
            throw new AssertionError("AI should not be called");
        }
    }
}
