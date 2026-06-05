package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IssueDto {
    private String code;
    private String message;
    private String filePath;
    private String snippet;
    private String severity;
}
