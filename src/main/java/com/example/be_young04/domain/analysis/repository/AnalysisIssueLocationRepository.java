package com.example.be_young04.domain.analysis.repository;

import com.example.be_young04.domain.analysis.entity.AnalysisIssueLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisIssueLocationRepository extends JpaRepository<AnalysisIssueLocation, Long> {

    List<AnalysisIssueLocation> findByAnalysisIssueIdIn(List<Long> analysisIssueIds);
}
