package com.example.be_young04.domain.realtime_analysis.service;

import com.example.be_young04.domain.analysis.checker.WcagCheckResult;
import com.example.be_young04.domain.analysis.checker.WcagChecker;
import com.example.be_young04.domain.analysis.checker.WcagCheckerRegistry;
import com.example.be_young04.domain.realtime_analysis.dto.IssueDetailDto;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.wcag.entity.WcagItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealtimeAnalysisService {

    private static final String DEFAULT_FILE_PATH = "index.html";

    private final WcagCheckerRegistry wcagCheckerRegistry;
    private final WcagItemMetadataService wcagItemMetadataService;

    // 실시간 분석은 공통 룰 엔진의 정적 검사만 실행하고 AI 호출이나 DB 저장은 하지 않는다.
    public RealtimeAnalysisResponse analyzeCode(String code, String filePath) {
        if (code == null || code.isBlank()) {
            return buildResponse(List.of());
        }

        String targetFilePath = filePath == null || filePath.isBlank()
                ? DEFAULT_FILE_PATH
                : filePath;

        List<WcagCheckResult> violations = wcagCheckerRegistry.getCheckersFor(targetFilePath).stream()
                .map(checker -> runChecker(checker, targetFilePath, code))
                .filter(result -> result != null
                        && result.getJudgeType() == WcagCheckResult.JudgeType.CODE
                        && Boolean.TRUE.equals(result.getViolated()))
                .toList();

        if (violations.isEmpty()) {
            return buildResponse(List.of());
        }

        Map<Long, WcagItem> wcagItems = wcagItemMetadataService.getCachedWcagItems();
        List<IssueDetailDto> issues = new ArrayList<>();
        for (WcagCheckResult violation : violations) {
            WcagItem wcagItem = wcagItems.get(violation.getWcagItemId());
            if (wcagItem != null) {
                issues.addAll(toIssueDetails(violation, wcagItem, targetFilePath));
            }
        }

        return buildResponse(issues);
    }

    private WcagCheckResult runChecker(WcagChecker checker, String filePath, String code) {
        return checker.check(filePath, code);
    }

    private List<IssueDetailDto> toIssueDetails(
            WcagCheckResult result,
            WcagItem wcagItem,
            String filePath
    ) {
        List<WcagCheckResult.IssueLocation> locations = result.getLocations();
        if (locations == null || locations.isEmpty()) {
            return List.of(buildIssueDetail(result, wcagItem, filePath, null));
        }

        return locations.stream()
                .map(location -> buildIssueDetail(result, wcagItem, filePath, location))
                .toList();
    }

    private IssueDetailDto buildIssueDetail(
            WcagCheckResult result,
            WcagItem wcagItem,
            String filePath,
            WcagCheckResult.IssueLocation location
    ) {
        String suggestion = location != null && location.getSuggestion() != null
                ? location.getSuggestion()
                : result.getMessage();

        return IssueDetailDto.builder()
                .wcagItemId(wcagItem.getWcagItemId())
                .sc(wcagItem.getSc())
                .title(wcagItem.getTitle())
                .levelType(wcagItem.getLevelType())
                .description(wcagItem.getDescription())
                .status("FAIL")
                .targetFilePath(filePath)
                .targetSelector(location != null ? location.getCssSelector() : null)
                .originalCodeBlock(location != null ? location.getViolatedCode() : null)
                .suggestion(suggestion)
                .measuredValue(result.getMessage())
                .thresholdValue("정적 룰 위반 없음")
                .build();
    }

    private RealtimeAnalysisResponse buildResponse(List<IssueDetailDto> issues) {
        return RealtimeAnalysisResponse.builder()
                .success(true)
                .timestamp(LocalDateTime.now())
                .issueCount(issues.size())
                .issues(issues)
                .build();
    }
}
