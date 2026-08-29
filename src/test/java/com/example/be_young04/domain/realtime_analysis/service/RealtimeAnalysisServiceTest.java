package com.example.be_young04.domain.realtime_analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import com.example.be_young04.domain.analysis.checker.WcagCheckerRegistry;
import com.example.be_young04.domain.realtime_analysis.dto.IssueDetailDto;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.wcag.entity.WcagItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeAnalysisServiceTest {

    private WcagCheckerRegistry wcagCheckerRegistry;
    private WcagItemMetadataService wcagItemMetadataService;
    private RealtimeAnalysisService realtimeAnalysisService;

    @BeforeEach
    void setUp() {
        wcagCheckerRegistry = mock(WcagCheckerRegistry.class);
        wcagItemMetadataService = mock(WcagItemMetadataService.class);
        realtimeAnalysisService = new RealtimeAnalysisService(
                wcagCheckerRegistry,
                wcagItemMetadataService
        );
    }

    @Test
    void analyzeCodeReturnsOnlyCodeRuleViolations() {
        String filePath = "src/App.tsx";
        String code = "<img src=\"logo.png\" />";

        WcagChecker violatedCodeChecker = checkerReturning(WcagCheckResult.builder()
                .wcagId("1.1.1")
                .wcagItemId(1L)
                .judgeType(WcagCheckResult.JudgeType.CODE)
                .violated(true)
                .message("이미지 대체 텍스트를 확인합니다.")
                .locations(List.of(
                        WcagCheckResult.IssueLocation.builder()
                                .cssSelector("img")
                                .violatedCode("<img src=\"logo.png\" />")
                                .suggestion("alt 속성을 추가하세요.")
                                .build(),
                        WcagCheckResult.IssueLocation.builder()
                                .cssSelector("img.logo")
                                .violatedCode("<img class=\"logo\" />")
                                .suggestion("alt 속성을 추가하세요.")
                                .build()
                ))
                .build());
        WcagChecker passedCodeChecker = checkerReturning(WcagCheckResult.builder()
                .wcagItemId(2L)
                .judgeType(WcagCheckResult.JudgeType.CODE)
                .violated(false)
                .build());
        WcagChecker codeAiChecker = checkerReturning(WcagCheckResult.builder()
                .wcagItemId(86L)
                .judgeType(WcagCheckResult.JudgeType.CODE_AI)
                .violated(null)
                .build());

        when(wcagCheckerRegistry.getCheckersFor(filePath))
                .thenReturn(List.of(violatedCodeChecker, passedCodeChecker, codeAiChecker));
        when(wcagItemMetadataService.getCachedWcagItems())
                .thenReturn(Map.of(1L, wcagItem(1L, "1.1.1", "비텍스트 콘텐츠")));

        RealtimeAnalysisResponse response = realtimeAnalysisService.analyzeCode(code, filePath);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueCount()).isEqualTo(2);
        assertThat(response.getIssues())
                .extracting(IssueDetailDto::getWcagItemId)
                .containsOnly(1L);
        assertThat(response.getIssues())
                .extracting(IssueDetailDto::getTargetSelector)
                .containsExactly("img", "img.logo");
        assertThat(response.getIssues().getFirst().getTargetFilePath()).isEqualTo(filePath);
        assertThat(response.getIssues().getFirst().getSuggestion()).isEqualTo("alt 속성을 추가하세요.");
    }

    @Test
    void analyzeCodeUsesDefaultHtmlFilePath() {
        WcagChecker checker = checkerReturning(WcagCheckResult.builder()
                .wcagItemId(1L)
                .judgeType(WcagCheckResult.JudgeType.CODE)
                .violated(false)
                .build());
        when(wcagCheckerRegistry.getCheckersFor("index.html")).thenReturn(List.of(checker));

        RealtimeAnalysisResponse response = realtimeAnalysisService.analyzeCode("<main></main>", " ");

        assertThat(response.getIssueCount()).isZero();
        verify(wcagCheckerRegistry).getCheckersFor("index.html");
        verify(checker).check("index.html", "<main></main>");
        verify(wcagItemMetadataService, never()).getCachedWcagItems();
    }

    @Test
    void analyzeCodeSkipsAllDependenciesForBlankCode() {
        RealtimeAnalysisResponse response = realtimeAnalysisService.analyzeCode(" ", "index.html");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueCount()).isZero();
        verify(wcagCheckerRegistry, never()).getCheckersFor("index.html");
        verify(wcagItemMetadataService, never()).getCachedWcagItems();
    }

    @Test
    void analyzeCodeKeepsSeparateMetadataForRulesWithTheSameSuccessCriterion() {
        WcagChecker firstRule = checkerReturning(codeViolation(1L, "첫 번째 위반"));
        WcagChecker secondRule = checkerReturning(codeViolation(2L, "두 번째 위반"));
        when(wcagCheckerRegistry.getCheckersFor("index.html"))
                .thenReturn(List.of(firstRule, secondRule));
        when(wcagItemMetadataService.getCachedWcagItems()).thenReturn(Map.of(
                1L, wcagItem(1L, "1.1.1", "비텍스트 콘텐츠"),
                2L, wcagItem(2L, "1.1.1", "장식 이미지")
        ));

        RealtimeAnalysisResponse response = realtimeAnalysisService.analyzeCode(
                "<img src=\"image.png\" />",
                null
        );

        assertThat(response.getIssues())
                .extracting(IssueDetailDto::getWcagItemId, IssueDetailDto::getTitle)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, "비텍스트 콘텐츠"),
                        org.assertj.core.groups.Tuple.tuple(2L, "장식 이미지")
                );
    }

    @Test
    void analyzeCodeUsesAccessibilityRulesRegisteredInTheCommonRegistry() {
        WcagItemMetadataService metadataService = mock(WcagItemMetadataService.class);
        when(metadataService.getCachedWcagItems())
                .thenReturn(Map.of(1L, wcagItem(1L, "1.1.1", "비텍스트 콘텐츠")));
        RealtimeAnalysisService service = new RealtimeAnalysisService(
                new WcagCheckerRegistry(List.of()),
                metadataService
        );

        RealtimeAnalysisResponse response = service.analyzeCode(
                "<main><img src=\"logo.png\" /></main>",
                "index.html"
        );

        assertThat(response.getIssues())
                .extracting(IssueDetailDto::getWcagItemId)
                .containsExactly(1L);
        assertThat(response.getIssues().getFirst().getOriginalCodeBlock())
                .contains("<img src=\"logo.png\"");
    }

    private WcagChecker checkerReturning(WcagCheckResult result) {
        WcagChecker checker = mock(WcagChecker.class);
        when(checker.check(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(result);
        return checker;
    }

    private WcagCheckResult codeViolation(Long wcagItemId, String message) {
        return WcagCheckResult.builder()
                .wcagItemId(wcagItemId)
                .judgeType(WcagCheckResult.JudgeType.CODE)
                .violated(true)
                .message(message)
                .locations(List.of())
                .build();
    }

    private WcagItem wcagItem(Long id, String sc, String title) {
        return WcagItem.builder()
                .wcagItemId(id)
                .sc(sc)
                .title(title)
                .levelType("A")
                .category("VISUAL")
                .description(title + " 설명")
                .build();
    }
}
