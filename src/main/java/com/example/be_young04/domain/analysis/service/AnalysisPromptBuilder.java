package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;
import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import org.springframework.stereotype.Component;

@Component
public class AnalysisPromptBuilder {

    public String build(
            String repositoryUrl,
            String deploymentUrl,
            String fileName,
            String code,
            CodeAnalysisResult parsingResult,
            SnapshotResponse snapshotResponse
    ) {
        String safeCode = code == null ? "" : code;

        if (safeCode.length() > 3000) {
            safeCode = safeCode.substring(0, 3000);
        }

        return """
                당신은 웹 프론트엔드 코드와 UI/UX를 분석하는 시니어 리뷰어입니다.
                아래 저장소 정보, 코드 파싱 결과, 렌더링 스냅샷 정보를 바탕으로 분석 리포트를 작성하세요.

                [저장소 URL]
                %s

                [배포 URL]
                %s

                [분석 파일명]
                %s

                [코드 파싱 결과]
                - classes: %s
                - methods/functions: %s
                - imports: %s
                - components: %s
                - jsxElements: %s
                - lineCount: %d

                [렌더링 스냅샷 정보]
                - snapshotPath: %s
                - viewport: %dx%d

                [코드 일부]
                ```jsx
                %s
                ```

                반드시 아래 형식으로 한국어로 답변하세요.

                1. 전체 요약
                2. 코드 구조 분석
                3. 컴포넌트/함수 역할 추정
                4. UI/UX 관점 분석
                5. 접근성 관점에서 개선할 점
                6. 우선 수정 항목 3개
                """.formatted(
                repositoryUrl,
                deploymentUrl,
                fileName,
                parsingResult.getClasses(),
                parsingResult.getMethods(),
                parsingResult.getImports(),
                parsingResult.getComponents(),
                parsingResult.getJsxElements(),
                parsingResult.getLineCount(),
                snapshotResponse.getImagePath(),
                snapshotResponse.getWidth(),
                snapshotResponse.getHeight(),
                safeCode
        );
    }
}