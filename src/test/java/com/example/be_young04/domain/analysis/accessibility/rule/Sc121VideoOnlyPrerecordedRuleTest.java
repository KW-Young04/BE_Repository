package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sc121VideoOnlyPrerecordedRuleTest {

    private final Sc121VideoOnlyPrerecordedRule rule = new Sc121VideoOnlyPrerecordedRule();

    @Test
    void analyzeFailsWhenVideoHasNoAlternative() {
        String code = """
                <section>
                  <video controls src="/demo.mp4"></video>
                </section>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getSuccessCriteria()).isEqualTo("1.2.1");
        assertThat(result.getStatus()).isEqualTo("FAIL");
        assertThat(result.getIssues())
                .extracting("type")
                .containsExactly("VIDEO_ALTERNATIVE_MISSING");
    }

    @Test
    void analyzePassesWhenVideoHasDescriptionsTrack() {
        String code = """
                <video controls>
                  <source src="/demo.mp4" type="video/mp4" />
                  <track kind="descriptions" src="/demo-description.vtt" srcLang="ko" label="Korean" />
                </video>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getIssues()).isEmpty();
    }

    @Test
    void analyzePassesWhenAdjacentTranscriptLinkExists() {
        String code = """
                <section>
                  <video controls src="/demo.mp4" />
                  <a href="/demo-transcript.html">video transcript</a>
                </section>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getIssues()).isEmpty();
    }
}
