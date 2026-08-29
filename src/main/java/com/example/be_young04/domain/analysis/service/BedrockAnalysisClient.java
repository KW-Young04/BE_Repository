package com.example.be_young04.domain.analysis.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BedrockAnalysisClient implements AiAnalysisClient {

    @Value("${bedrock.region}")
    private String region;

    @Value("${bedrock.model-id}")
    private String modelId;

    private BedrockRuntimeClient client;

    @PostConstruct
    private void init() {
        this.client = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String analyze(String prompt, List<byte[]> imageBytesList) {
        String optimizedPrompt = optimizePrompt(prompt);

        List<ContentBlock> contentBlocks = new ArrayList<>();
        contentBlocks.add(ContentBlock.fromText(optimizedPrompt));

        if (imageBytesList != null) {
            for (byte[] imageBytes : imageBytesList) {
                if (imageBytes == null || imageBytes.length == 0) continue;

                contentBlocks.add(ContentBlock.fromImage(
                        ImageBlock.builder()
                                .format(ImageFormat.PNG)
                                .source(ImageSource.fromBytes(SdkBytes.fromByteArray(imageBytes)))
                                .build()
                ));
            }
        }

        Message message = Message.builder()
                .role(ConversationRole.USER)
                .content(contentBlocks)
                .build();

        ConverseRequest request = ConverseRequest.builder()
                .modelId(modelId)
                .messages(List.of(message))
                .build();

        int maxRetry = 3;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                ConverseResponse response = client.converse(request);
                return extractText(response);

            } catch (ThrottlingException e) {
                sleep(attempt);
                if (attempt == maxRetry) {
                    return "Bedrock API 사용량 한도를 초과했습니다. 잠시 후 다시 시도하거나 쿼터 설정을 확인하세요.";
                }

            } catch (ServiceUnavailableException | ModelTimeoutException e) {
                sleep(attempt);
                if (attempt == maxRetry) {
                    return "Bedrock 모델 서버가 현재 과부하 상태입니다. 잠시 후 다시 시도하세요.";
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

    private String extractText(ConverseResponse response) {
        if (response == null
                || response.output() == null
                || response.output().message() == null
                || response.output().message().content() == null
                || response.output().message().content().isEmpty()
                || response.output().message().content().get(0).text() == null) {
            return "Bedrock 분석 결과를 가져오지 못했습니다.";
        }

        return response.output().message().content().get(0).text();
    }
}