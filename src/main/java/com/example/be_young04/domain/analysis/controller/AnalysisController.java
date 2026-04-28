package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.dto.CodeAnalyzeRequest;
import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;
import com.example.be_young04.domain.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public CodeAnalysisResult analyze(@RequestBody CodeAnalyzeRequest request) {
        return analysisService.analyze(
                request.getFileName(),
                request.getCode()
        );
    }
}