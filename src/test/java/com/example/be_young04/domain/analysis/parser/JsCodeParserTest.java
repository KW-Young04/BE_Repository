package com.example.be_young04.domain.analysis.parser;

import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsCodeParserTest {

    private final JsCodeParser parser = new JsCodeParser();

    @Test
    void parseDetectsSc111Failures() {
        String code = """
                export default function App() {
                  return (
                    <main>
                      <img src="/logo.png" />
                      <img src="/empty.png" alt="" />
                      <video src="/intro.mp4"></video>
                      <audio src="/guide.mp3" />
                    </main>
                  );
                }
                """;

        CodeAnalysisResult result = parser.parse(code);

        assertThat(result.getAccessibilityChecks()).hasSize(1);
        assertThat(result.getAccessibilityChecks().get(0).getSuccessCriteria()).isEqualTo("1.1.1");
        assertThat(result.getAccessibilityChecks().get(0).getLevel()).isEqualTo("A");
        assertThat(result.getAccessibilityChecks().get(0).getStatus()).isEqualTo("FAIL");
        assertThat(result.getAccessibilityChecks().get(0).getIssues())
                .extracting("type")
                .containsExactly("IMG_ALT_MISSING", "IMG_ALT_EMPTY", "MEDIA_TRACK_MISSING", "MEDIA_TRACK_MISSING");
    }

    @Test
    void parsePassesSc111WhenAlternativesExist() {
        String code = """
                function App() {
                  return (
                    <>
                      <img src="/logo.png" alt="서비스 로고" />
                      <video controls>
                        <source src="/intro.mp4" type="video/mp4" />
                        <track kind="captions" src="/intro.vtt" srcLang="ko" label="Korean" />
                      </video>
                    </>
                  );
                }
                """;

        CodeAnalysisResult result = parser.parse(code);

        assertThat(result.getAccessibilityChecks()).hasSize(1);
        assertThat(result.getAccessibilityChecks().get(0).getStatus()).isEqualTo("PASS");
        assertThat(result.getAccessibilityChecks().get(0).getIssues()).isEmpty();
        assertThat(result.getAccessibilityIssues()).isEmpty();
    }
}
