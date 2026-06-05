package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccessibilityIssue {
    private String code;
    private String message;
    private int startLine;
    private int endLine;
    private String snippet;
}
