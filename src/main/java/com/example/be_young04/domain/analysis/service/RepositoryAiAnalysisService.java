package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.dto.AiAnalysisRequest;
import com.example.be_young04.domain.analysis.dto.AiAnalysisResponse;
import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;
import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import com.example.be_young04.domain.snapshot.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RepositoryAiAnalysisService {

    private final AnalysisService analysisService;
    private final SnapshotService snapshotService;
    private final AnalysisPromptBuilder analysisPromptBuilder;
    private final AiAnalysisClient aiAnalysisClient;

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        CodeAnalysisResult parsingResult = analysisService.analyze(
                request.getFileName(),
                request.getCode()
        );

        SnapshotResponse snapshotResponse = snapshotService.capture(
                request.getDeploymentUrl()
        );

        String prompt = analysisPromptBuilder.build(
                request.getRepositoryUrl(),
                request.getDeploymentUrl(),
                request.getFileName(),
                request.getCode(),
                parsingResult,
                snapshotResponse
        );

        String aiReport = aiAnalysisClient.analyze(prompt);

        return AiAnalysisResponse.builder()
                .codeParsingResult(parsingResult)
                .snapshotPath(snapshotResponse.getImagePath())
                .aiReport(aiReport)
                .build();
    }
}