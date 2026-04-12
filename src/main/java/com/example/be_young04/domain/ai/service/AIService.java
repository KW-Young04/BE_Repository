package com.example.be_young04.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.be_young04.domain.ai.client.AIClient;

@Service
@RequiredArgsConstructor
public class AIService {

    private final AIClient AIClient;

    public String analyze(String prompt) {
        return AIClient.ask(prompt);
    }
}