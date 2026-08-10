package com.example.be_young04.domain.analysis.service;

import java.util.List;

public interface AiAnalysisClient {
    String analyze(String prompt, List<byte[]> imageBytesList);
}