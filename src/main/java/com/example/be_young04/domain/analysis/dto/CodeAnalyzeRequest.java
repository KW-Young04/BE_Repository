package com.example.be_young04.domain.analysis.dto;

import lombok.Getter;

@Getter
public class CodeAnalyzeRequest {
    private String fileName;
    private String code;
}