package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.dto.GeminiRequest;
import com.example.be_young04.domain.analysis.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GeminiAnalysisClient implements AiAnalysisClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Override
    public String analyze(String prompt) {
        RestClient restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        GeminiRequest request = GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        GeminiResponse response = restClient.post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        return extractText(response);
    }

    private String extractText(GeminiResponse response) {
        if (response == null
                || response.getCandidates() == null
                || response.getCandidates().isEmpty()
                || response.getCandidates().get(0).getContent() == null
                || response.getCandidates().get(0).getContent().getParts() == null
                || response.getCandidates().get(0).getContent().getParts().isEmpty()
                || response.getCandidates().get(0).getContent().getParts().get(0).getText() == null) {
            return "Gemini 분석 결과를 가져오지 못했습니다.";
        }

        return response.getCandidates()
                .get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();
    }
}