package com.example.be_young04.domain.analysis.parser;

import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;

public interface CodeParser {
    CodeAnalysisResult parse(String code);
}