package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.AccessibilityIssue;
import com.example.be_young04.domain.analysis.rule.Sc111DecorativeContentRule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sc111DecorativeContentRuleTest {

    private final Sc111DecorativeContentRule rule = new Sc111DecorativeContentRule();

    @Test
    void analyzePassesWhenDecorativeImagesAreIgnored() {
        String code = """
                <div>
                  <img src="/divider.png" alt="" />
                  <img src="/spacer.png" role="presentation" />
                  <img src="/shape.png" role="none" />
                </div>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getSuccessCriteria()).isEqualTo("1.1.1");
        assertThat(result.getName()).isEqualTo("이미지 대체 텍스트 - 순수 장식");
        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getIssues()).isEmpty();
    }

    @Test
    void analyzeFailsWhenDecorativeImageIsNotIgnored() {
        String code = """
                <div>
                  <img src="/divider.png" />
                  <img src="/ornament.png" alt="장식 선" />
                </div>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("FAIL");
        assertThat(result.getIssues())
                .extracting(AccessibilityIssue::getCode)
                .containsExactly("DECORATIVE_IMAGE_NOT_IGNORED", "DECORATIVE_IMAGE_NOT_IGNORED");
    }
}
