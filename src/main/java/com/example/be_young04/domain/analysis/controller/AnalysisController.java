package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.dto.CodeAnalyzeRequest;
import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;
import com.example.be_young04.domain.analysis.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Code Analysis", description = "\uCF54\uB4DC \uAD6C\uC870 \uBC0F \uC811\uADFC\uC131 \uBD84\uC11D API")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "\uCF54\uB4DC \uC804\uCCB4 \uBD84\uC11D")
    @PostMapping
    public CodeAnalysisResult analyze(@RequestBody CodeAnalyzeRequest request) {
        return analysisService.analyze(
                request.getFileName(),
                request.getCode()
        );
    }
}
