package com.example.be_young04.domain.analysis.repository;

import com.example.be_young04.domain.analysis.entity.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
}