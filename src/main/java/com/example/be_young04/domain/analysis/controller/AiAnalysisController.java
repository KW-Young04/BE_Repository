package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.dto.AiAnalysisRequest;
import com.example.be_young04.domain.analysis.dto.AiAnalysisResponse;
import com.example.be_young04.domain.analysis.service.RepositoryAiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis/ai")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final RepositoryAiAnalysisService repositoryAiAnalysisService;

    @PostMapping
    public AiAnalysisResponse analyze(@RequestBody AiAnalysisRequest request) {
        return repositoryAiAnalysisService.analyze(request);
    }
}