package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.service.WcagAnalysisService;
import com.example.be_young04.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "WCAG Analysis", description = "WCAG 접근성 분석 API")
@RestController
@RequestMapping("/api/analysis/wcag")
@RequiredArgsConstructor
public class WcagAnalysisController {

    private final WcagAnalysisService wcagAnalysisService;

    @Operation(
            summary = "WCAG 접근성 분석 실행",
            description = "GitHub 저장소를 분석하여 WCAG 위반 항목을 DB에 저장하고 결과 ID를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> analyze(
            @AuthenticationPrincipal Long githubId,
            @RequestBody WcagAnalysisRequest request
    ) {
        Long resultId = wcagAnalysisService.analyze(
                githubId,
                request.repositoryUrl(),
                request.deploymentUrl(),
                request.branchName()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200WCAG001",
                        "WCAG 분석 완료",
                        resultId
                )
        );
    }

    record WcagAnalysisRequest(String repositoryUrl, String deploymentUrl, String branchName) {}
}