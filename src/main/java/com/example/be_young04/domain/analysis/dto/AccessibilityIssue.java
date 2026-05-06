package com.example.be_young04.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AccessibilityIssue {
    private String type;
    private String message;
    private int line;
    private String snippet;
}
