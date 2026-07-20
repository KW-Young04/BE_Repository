package com.example.be_young04.domain.realtime_analysis.controller;

import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisRequest;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.realtime_analysis.service.RealtimeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final RealtimeAnalysisService realtimeAnalysisService;

    /**
     * 프론트엔드 IDE 디바운스 전용 실시간 정적 분석 API
     * (DB 저장 X, WCAG_ITEMS 항목 설명 매핑 후 메모리 리턴)
     */
    @PostMapping("/realtime")
    public ResponseEntity<RealtimeAnalysisResponse> analyzeRealtime(
            @RequestBody RealtimeAnalysisRequest request) {

        RealtimeAnalysisResponse response = realtimeAnalysisService.analyzeCode(
                request.getCode(),
                request.getTargetFilePath()
        );

        return ResponseEntity.ok(response);
    }
}