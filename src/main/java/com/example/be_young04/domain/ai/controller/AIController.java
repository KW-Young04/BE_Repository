package com.example.be_young04.domain.ai.controller;

import com.example.be_young04.domain.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestBody String prompt) {
        String result = aiService.analyze(prompt);
        return ResponseEntity.ok(result);
    }
}