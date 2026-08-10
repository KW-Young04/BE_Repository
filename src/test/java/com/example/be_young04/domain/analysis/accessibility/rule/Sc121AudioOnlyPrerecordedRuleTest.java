package com.example.be_young04.domain.analysis.accessibility.rule;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.rule.Sc121AudioOnlyPrerecordedRule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sc121AudioOnlyPrerecordedRuleTest {

    private final Sc121AudioOnlyPrerecordedRule rule = new Sc121AudioOnlyPrerecordedRule();

    @Test
    void analyzeFailsWhenAudioHasNoTextAlternative() {
        String code = """
                <section>
                  <audio controls src="/interview.mp3"></audio>
                </section>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getSuccessCriteria()).isEqualTo("1.2.1");
        assertThat(result.getStatus()).isEqualTo("FAIL");
        assertThat(result.getIssues())
                .extracting("type")
                .containsExactly("AUDIO_ALTERNATIVE_MISSING");
    }

    @Test
    void analyzePassesWhenAudioHasTrack() {
        String code = """
                <audio controls>
                  <source src="/interview.mp3" type="audio/mpeg" />
                  <track kind="captions" src="/interview.vtt" srcLang="ko" label="Korean" />
                </audio>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getIssues()).isEmpty();
    }

    @Test
    void analyzePassesWhenAdjacentTranscriptLinkExists() {
        String code = """
                <section>
                  <audio controls src="/interview.mp3" />
                  <a href="/interview-transcript.html">transcript</a>
                </section>
                """;

        AccessibilityCheckResult result = rule.analyze(code);

        assertThat(result.getStatus()).isEqualTo("PASS");
        assertThat(result.getIssues()).isEmpty();
    }
}
