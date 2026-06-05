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
            summary = "1.1.1 \uCEE8\uD2B8\uB864/\uC785\uB825 \uC811\uADFC\uC131 \uCF54\uB4DC+AI \uAC80\uC0AC",
            description = "\uCEE8\uD2B8\uB864 \uC5ED\uD560\uC758 \uBE44\uD14D\uC2A4\uD2B8 \uCF58\uD150\uCE20\uC5D0 aria-label, aria-labelledby, title, alt \uB4F1 accessible name \uD6C4\uBCF4\uAC00 \uC788\uB294\uC9C0 \uCF54\uB4DC\uB85C \uD655\uC778\uD558\uACE0, \uBAA9\uC801 \uC124\uBA85 \uD488\uC9C8\uC740 AI \uAC80\uD1A0 \uD544\uC694\uB85C \uD45C\uC2DC\uD569\uB2C8\uB2E4."
    )
    @PostMapping("/sc-1-1-1/control-input")
    public AccessibilityCheckResult analyzeSc111ControlInput(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc111ControlInput(request.getCode());
    }

    @Operation(
            summary = "1.2.1 \uC624\uB514\uC624 \uC804\uC6A9 \uC811\uADFC\uC131 \uC815\uC801 \uAC80\uC0AC",
            description = "\uC0AC\uC804\uB179\uC74C audio \uD0DC\uADF8\uC758 track \uD0DC\uADF8 \uB610\uB294 \uC778\uC811\uD55C \uD14D\uC2A4\uD2B8 \uB300\uBCF8 \uB9C1\uD06C \uC874\uC7AC \uC5EC\uBD80\uB97C \uC815\uC801 \uBD84\uC11D\uD569\uB2C8\uB2E4."
    )
    @PostMapping("/sc-1-2-1/audio-only-prerecorded")
    public AccessibilityCheckResult analyzeSc121Audio(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc121Audio(request.getCode());
    }

    @PostMapping("/sc-1-2-1/video-only-prerecorded")
    public AccessibilityCheckResult analyzeSc121VideoOnly(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc121VideoOnly(request.getCode());
    }

    @Operation(summary = "1.2.2 자막 제공 (사전녹음)")
    @PostMapping("/sc-1-2-2")
    public AccessibilityCheckResult analyzeSc122(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc122(request.getCode());
    }

    @Operation(summary = "1.2.3 음성 해설 또는 미디어 대안 (사전녹음)")
    @PostMapping("/sc-1-2-3")
    public AccessibilityCheckResult analyzeSc123(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc123(request.getCode());
    }

    @Operation(summary = "1.3.1 정보와 관계 (구조적 마크업)")
    @PostMapping("/sc-1-3-1")
    public AccessibilityCheckResult analyzeSc131(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc131(request.getCode());
    }

    @Operation(summary = "1.4.1 색상 사용 - 색상 인식 전용")
    @PostMapping("/sc-1-4-1")
    public AccessibilityCheckResult analyzeSc141(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc141(request.getCode());
    }

    @Operation(summary = "1.4.2 오디오 제어 - 비간섭 요건")
    @PostMapping("/sc-1-4-2")
    public AccessibilityCheckResult analyzeSc142(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc142(request.getCode());
    }

    @Operation(summary = "2.1.2 키보드 트랩 없음")
    @PostMapping("/sc-2-1-2")
    public AccessibilityCheckResult analyzeSc212(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc212(request.getCode());
    }

    @Operation(summary = "2.2.1 타이밍 조정")
    @PostMapping("/sc-2-2-1")
    public AccessibilityCheckResult analyzeSc221(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc221(request.getCode());
    }

    @Operation(summary = "2.2.2 일시정지/정지/숨기기 - 자동 업데이트")
    @PostMapping("/sc-2-2-2")
    public AccessibilityCheckResult analyzeSc222(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc222(request.getCode());
    }

    @Operation(summary = "2.3.1 세 번 이하 점멸")
    @PostMapping("/sc-2-3-1")
    public AccessibilityCheckResult analyzeSc231(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc231(request.getCode());
    }

    @Operation(summary = "2.4.1 블록 건너뛰기")
    @PostMapping("/sc-2-4-1")
    public AccessibilityCheckResult analyzeSc241(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc241(request.getCode());
    }

    @Operation(summary = "2.4.2 페이지 제목")
    @PostMapping("/sc-2-4-2")
    public AccessibilityCheckResult analyzeSc242(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc242(request.getCode());
    }

    @Operation(summary = "2.4.3 포커스 순서")
    @PostMapping("/sc-2-4-3")
    public AccessibilityCheckResult analyzeSc243(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc243(request.getCode());
    }

    @Operation(summary = "3.1.1 페이지 언어")
    @PostMapping("/sc-3-1-1")
    public AccessibilityCheckResult analyzeSc311(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc311(request.getCode());
    }

    @Operation(summary = "3.2.1 포커스 시 변경 없음")
    @PostMapping("/sc-3-2-1")
    public AccessibilityCheckResult analyzeSc321(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc321(request.getCode());
    }

    @Operation(summary = "3.2.2 입력 시 변경 없음")
    @PostMapping("/sc-3-2-2")
    public AccessibilityCheckResult analyzeSc322(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc322(request.getCode());
    }

    @Operation(summary = "3.3.1 오류 식별")
    @PostMapping("/sc-3-3-1")
    public AccessibilityCheckResult analyzeSc331(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc331(request.getCode());
    }

    @Operation(summary = "3.3.2 레이블 또는 설명")
    @PostMapping("/sc-3-3-2")
    public AccessibilityCheckResult analyzeSc332(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc332(request.getCode());
    }

    @Operation(summary = "4.1.2 이름·역할·값")
    @PostMapping("/sc-4-1-2")
    public AccessibilityCheckResult analyzeSc412(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc412(request.getCode());
    }
}
