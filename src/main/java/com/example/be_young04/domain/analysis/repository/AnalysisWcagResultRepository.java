package com.example.be_young04.domain.analysis.repository;

import com.example.be_young04.domain.analysis.entity.AnalysisWcagResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisWcagResultRepository extends JpaRepository<AnalysisWcagResult, Long> {

    List<AnalysisWcagResult> findByRepositoryId(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);
}