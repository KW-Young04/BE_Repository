package com.example.be_young04.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class WcagResultDto {
    private String successCriteria;
    private String title;
    private String level;
    private String status;
    private List<IssueDto> issues;
}
