package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.accessibility.AccessibilityAnalysisService;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.CodeAnalyzeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Accessibility Analysis", description = "\uC6F9 \uC811\uADFC\uC131 \uC815\uC801 \uBD84\uC11D API")
@RestController
@RequestMapping("/api/analysis/accessibility")
@RequiredArgsConstructor
public class AccessibilityAnalysisController {

    private final AccessibilityAnalysisService accessibilityAnalysisService;

    @Operation(
            summary = "1.1.1 \uC811\uADFC\uC131 \uC815\uC801 \uAC80\uC0AC",
            description = "img alt \uC18D\uC131\uACFC video/audio \uB0B4\uBD80 track \uD0DC\uADF8 \uC874\uC7AC \uC5EC\uBD80\uB97C \uC815\uC801 \uBD84\uC11D\uD569\uB2C8\uB2E4."
    )
    @PostMapping("/sc-1-1-1")
    public AccessibilityCheckResult analyzeSc111(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc111(request.getCode());
    }

    @Operation(
            summary = "1.1.1 \uC21C\uC218 \uC7A5\uC2DD \uC811\uADFC\uC131 \uC815\uC801 \uAC80\uC0AC",
            description = "\uC7A5\uC2DD\uC6A9 img\uAC00 alt=\"\" \uB610\uB294 role=\"presentation\"/role=\"none\"\uC73C\uB85C \uBCF4\uC870 \uAE30\uC220\uC5D0\uC11C \uBB34\uC2DC\uB418\uB294\uC9C0 \uC815\uC801 \uBD84\uC11D\uD569\uB2C8\uB2E4."
    )
    @PostMapping("/sc-1-1-1/decorative")
    public AccessibilityCheckResult analyzeSc111Decorative(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc111Decorative(request.getCode());
    }

    @Operation(
            summary = "1.2.1 \uC624\uB514\uC624 \uC804\uC6A9 \uC811\uADFC\uC131 \uC815\uC801 \uAC80\uC0AC",
            description = "\uC0AC\uC804\uB179\uC74C audio \uD0DC\uADF8\uC758 track \uD0DC\uADF8 \uB610\uB294 \uC778\uC811\uD55C \uD14D\uC2A4\uD2B8 \uB300\uBCF8 \uB9C1\uD06C \uC874\uC7AC \uC5EC\uBD80\uB97C \uC815\uC801 \uBD84\uC11D\uD569\uB2C8\uB2E4."
    )
    @PostMapping("/sc-1-2-1/audio-only-prerecorded")
    public AccessibilityCheckResult analyzeSc121(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc121(request.getCode());
    }
}
