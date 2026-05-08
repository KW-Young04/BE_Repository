package com.example.be_young04.domain.analysis.service;

public interface AiAnalysisClient {
    String analyze(String prompt, byte[] imageBytes);
}