package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;
import com.example.be_young04.domain.analysis.parser.CodeParser;
import com.example.be_young04.domain.analysis.parser.ParserFactory;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    public CodeAnalysisResult analyze(String fileName, String code) {
        CodeParser parser = ParserFactory.getParser(fileName);
        return parser.parse(code);
    }
}