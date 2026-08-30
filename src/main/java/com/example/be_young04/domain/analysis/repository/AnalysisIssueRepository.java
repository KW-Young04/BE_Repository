package com.example.be_young04.domain.analysis.repository;

import com.example.be_young04.domain.analysis.entity.AnalysisIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisIssueRepository extends JpaRepository<AnalysisIssue, Long> {

    List<AnalysisIssue> findByAnalysisWcagResultIdIn(List<Long> analysisWcagResultIds);
}
