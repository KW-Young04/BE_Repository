package com.example.be_young04.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CodeAnalysisResult {
    private List<String> classes;
    private List<String> methods;
    private List<String> imports;
    private List<String> components;
    private List<String> jsxElements;
    private int lineCount;
}