package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sc111ControlInputNameRuleTest {

    private final Sc111ControlInputNameRule rule = new Sc111ControlInputNameRule();

    @Test
    void analyzeFailsWhenControlImagesHaveNoAccessibleName() {
        String code = """
                <div>
                  <input type="image" src="/search.png" />
                  <img src="/next.png" role="button" />
                  <button><img src="/submit.png" /></button>
                </div>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getSuccessCriteria()).isEqualTo("1.1.1");
        assertThat(result.getStatus()).isEqualTo("FAIL");
        assertThat(result.isAiReviewRequired()).isFalse();
        assertThat(result.getIssues())
                .extracting("type")
                .containsExactly("CONTROL_INPUT_NAME_MISSING", "CONTROL_IMAGE_NAME_MISSING", "CONTROL_IMAGE_NAME_MISSING");
    }

    @Test
    void analyzeRequiresAiReviewWhenAccessibleNameCandidatesExist() {
        String code = """
                <div>
                  <input type="image" src="/search.png" alt="검색" />
                  <img src="/next.png" role="button" aria-label="다음 페이지" />
                  <button aria-label="제출"><img src="/submit.png" /></button>
                  <svg role="button" aria-label="닫기"><path d="M0 0" /></svg>
                </div>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("NEEDS_AI_REVIEW");
        assertThat(result.isAiReviewRequired()).isTrue();
        assertThat(result.getIssues()).isEmpty();
    }

    @Test
    void analyzePassesWhenNoControlImagesExist() {
        String code = """
                <div>
                  <img src="/photo.png" alt="제품 사진" />
                </div>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.isAiReviewRequired()).isFalse();
        assertThat(result.getIssues()).isEmpty();
    }
}
