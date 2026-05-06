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

@Tag(name = "Accessibility Analysis", description = "웹 접근성 정적 분석 API")
@RestController
@RequestMapping("/api/analysis/accessibility")
@RequiredArgsConstructor
public class AccessibilityAnalysisController {

    private final AccessibilityAnalysisService accessibilityAnalysisService;

    @Operation(
            summary = "1.1.1 접근성 정적 검사",
            description = "img alt 속성과 video/audio 내부 track 태그 존재 여부를 정적 분석합니다."
    )
    @PostMapping("/sc-1-1-1")
    public AccessibilityCheckResult analyzeSc111(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc111(request.getCode());
    }

    @Operation(
            summary = "1.1.1 순수 장식 접근성 정적 검사",
            description = "장식용 img가 alt=\"\" 또는 role=\"presentation\"/role=\"none\"으로 보조 기술에서 무시되는지 정적 분석합니다."
    )
    @PostMapping("/sc-1-1-1/decorative")
    public AccessibilityCheckResult analyzeSc111Decorative(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc111Decorative(request.getCode());
    }

    @Operation(
            summary = "1.1.1 컨트롤/입력 접근성 코드+AI 검사",
            description = "컨트롤 역할의 비텍스트 콘텐츠에 aria-label, aria-labelledby, title, alt 등 accessible name 후보가 있는지 코드로 확인하고, 목적 설명 품질은 AI 검토 필요로 표시합니다."
    )
    @PostMapping("/sc-1-1-1/control-input")
    public AccessibilityCheckResult analyzeSc111ControlInput(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc111ControlInput(request.getCode());
    }

    @Operation(
            summary = "1.2.1 오디오 전용 접근성 정적 검사",
            description = "사전녹음 audio 태그의 track 태그 또는 인접한 텍스트 대본 링크 존재 여부를 정적 분석합니다."
    )
    @PostMapping("/sc-1-2-1/audio-only-prerecorded")
    public AccessibilityCheckResult analyzeSc121(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc121(request.getCode());
    }

    @Operation(
            summary = "1.2.1 비디오 전용 접근성 정적 검사",
            description = "사전녹음 video 태그의 track kind=\"descriptions\" 또는 인접한 텍스트 대본 링크 존재 여부를 정적 분석합니다."
    )
    @PostMapping("/sc-1-2-1/video-only-prerecorded")
    public AccessibilityCheckResult analyzeSc121VideoOnly(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc121VideoOnly(request.getCode());
    }

    @Operation(
            summary = "2.3.1 세 번 이하 점멸 검사",
            description = "CSS animation, @keyframes, opacity, visibility, red color 패턴을 분석하여 1초에 3회 초과 점멸 또는 적색 점멸 위험 여부를 검사합니다."
    )
    @PostMapping("/sc-2-3-1")
    public AccessibilityCheckResult analyzeSc231(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc231(request.getCode());
    }

    @Operation(
            summary = "2.4.1 블록 건너뛰기 검사",
            description = "skip-nav 링크 또는 main/nav/header 등 landmark 존재 여부를 분석하여 반복 콘텐츠를 건너뛸 수 있는지 검사합니다."
    )
    @PostMapping("/sc-2-4-1")
    public AccessibilityCheckResult analyzeSc241(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc241(request.getCode());
    }

    @Operation(
            summary = "2.4.2 페이지 제목 검사",
            description = "title 태그 존재 여부와 비어 있지 않은지 여부를 정적 분석합니다."
    )
    @PostMapping("/sc-2-4-2")
    public AccessibilityCheckResult analyzeSc242(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc242(request.getCode());
    }

    @Operation(
            summary = "2.4.3 포커스 순서 검사",
            description = "tabindex 값과 포커스 가능한 요소를 분석하여 키보드 탐색 순서가 왜곡될 가능성이 있는지 검사합니다."
    )
    @PostMapping("/sc-2-4-3")
    public AccessibilityCheckResult analyzeSc243(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc243(request.getCode());
    }

    @Operation(
            summary = "3.1.1 페이지 언어 검사",
            description = "html 태그의 lang 속성 존재 여부와 유효한 BCP 47 언어 코드 여부를 검사합니다."
    )
    @PostMapping("/sc-3-1-1")
    public AccessibilityCheckResult analyzeSc311(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc311(request.getCode());
    }

    @Operation(
            summary = "3.2.1 포커스 시 변경 없음 검사",
            description = "onFocus 이벤트에서 페이지 이동, 새 창 열기, form submit 등 컨텍스트 변경 코드 패턴을 검사합니다."
    )
    @PostMapping("/sc-3-2-1")
    public AccessibilityCheckResult analyzeSc321(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc321(request.getCode());
    }

    @Operation(
            summary = "3.2.2 입력 시 변경 없음 검사",
            description = "onChange 이벤트에서 즉시 form submit, 페이지 이동, 새 창 열기 등 컨텍스트 변경 코드 패턴을 검사합니다."
    )
    @PostMapping("/sc-3-2-2")
    public AccessibilityCheckResult analyzeSc322(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc322(request.getCode());
    }

    @Operation(
            summary = "3.3.1 오류 식별 검사",
            description = "required, aria-invalid, role=\"alert\", aria-describedby, 오류 메시지 패턴을 분석하여 입력 오류 식별 가능 여부를 검사합니다."
    )
    @PostMapping("/sc-3-3-1")
    public AccessibilityCheckResult analyzeSc331(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc331(request.getCode());
    }

    @Operation(
            summary = "3.3.2 레이블 또는 설명 검사",
            description = "input, textarea, select 요소에 label, placeholder, aria-label, aria-labelledby, aria-describedby 등의 레이블 또는 설명이 제공되는지 검사합니다."
    )
    @PostMapping("/sc-3-3-2")
    public AccessibilityCheckResult analyzeSc332(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc332(request.getCode());
    }

    @Operation(
            summary = "4.1.2 이름·역할·값 검사",
            description = "표준 HTML 컨트롤과 커스텀 컴포넌트의 role, accessible name, aria state 제공 여부를 검사합니다."
    )
    @PostMapping("/sc-4-1-2")
    public AccessibilityCheckResult analyzeSc412(@RequestBody CodeAnalyzeRequest request) {
        return accessibilityAnalysisService.analyzeSc412(request.getCode());
    }
}