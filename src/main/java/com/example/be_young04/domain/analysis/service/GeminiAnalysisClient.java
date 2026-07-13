package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.dto.GeminiRequest;
import com.example.be_young04.domain.analysis.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
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
    public String analyze(String prompt, List<byte[]> imageBytesList) {
        RestClient restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        String optimizedPrompt = optimizePrompt(prompt);

        List<GeminiRequest.Part> parts = new ArrayList<>();
        parts.add(GeminiRequest.Part.builder()
                .text(optimizedPrompt)
                .build());

        if (imageBytesList != null) {
            for (byte[] imageBytes : imageBytesList) {
                if (imageBytes == null || imageBytes.length == 0) continue;

                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                parts.add(GeminiRequest.Part.builder()
                        .inlineData(GeminiRequest.InlineData.builder()
                                .mimeType("image/png")
                                .data(base64Image)
                                .build())
                        .build());
            }
        }

        GeminiRequest request = GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(parts)
                                .build()
                ))
                .build();

        int maxRetry = 3;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                GeminiResponse response = restClient.post()
                        .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                        .body(request)
                        .retrieve()
                        .body(GeminiResponse.class);

                return extractText(response);

            } catch (HttpClientErrorException.TooManyRequests e) {
                sleep(attempt);
                if (attempt == maxRetry) {
                    return "Gemini API 사용량 한도를 초과했습니다. 잠시 후 다시 시도하거나 쿼터/결제 설정을 확인하세요.";
                }

            } catch (HttpServerErrorException.ServiceUnavailable e) {
                sleep(attempt);
                if (attempt == maxRetry) {
                    return "Gemini 모델 서버가 현재 과부하 상태입니다. 잠시 후 다시 시도하세요.";
                }

            } catch (Exception e) {
                return "AI 분석 중 오류가 발생했습니다: " + e.getMessage();
            }
        }

        return "AI 분석 결과를 가져오지 못했습니다.";
    }

    private String optimizePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "분석할 내용이 없습니다.";
        }

        return prompt
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(1000L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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