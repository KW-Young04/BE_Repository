package com.example.be_young04.domain.ai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.be_young04.domain.ai.dto.AIRequestDto;
import com.example.be_young04.domain.ai.dto.AIResponseDto;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AIClient {

    private final WebClient AIWebClient;

    public String ask(String prompt) {
        AIRequestDto request = new AIRequestDto(
                List.of(new AIRequestDto.Content(
                        List.of(new AIRequestDto.Part(prompt))
                ))
        );

        AIResponseDto response = AIWebClient
                .post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AIResponseDto.class)
                .block();

        return response.getCandidates().get(0)
                .getContent().getParts().get(0)
                .getText();
    }
}