package com.example.be_young04.domain.analysis.checker;

import java.util.List;

public interface WcagChecker {

    // 담당 WCAG 항목 ID (예: "1.1.1")
    String getWcagId();

    // 지원 파일 확장자 (예: "html", "tsx", "css")
    List<String> getSupportedExtensions();

    // 체크 실행
    WcagCheckResult check(String fileName, String fileContent);
}