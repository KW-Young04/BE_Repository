package com.example.be_young04.domain.analysis.controller;

import com.example.be_young04.domain.analysis.service.WcagAnalysisService;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.snapshot.dto.PageSnapshot;
import com.example.be_young04.domain.snapshot.dto.SnapshotMetaDto;
import com.example.be_young04.global.common.response.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "WCAG Analysis", description = "WCAG 접근성 분석 API")
@RestController
@RequestMapping("/api/analysis/wcag")
@RequiredArgsConstructor
public class WcagAnalysisController {

    private final WcagAnalysisService wcagAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(
            summary = "WCAG 접근성 분석 실행",
            description = "GitHub 저장소와 페이지별 스냅샷 목록을 분석하여 WCAG 위반 항목을 DB에 저장하고 결과 ID를 반환합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> analyze(
            @AuthenticationPrincipal Long githubId,
            @RequestParam("repositoryUrl") String repositoryUrl,
            @RequestParam("branchName") String branchName,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("snapshotMeta") String snapshotMetaJson
    ) {
        List<PageSnapshot> snapshots = buildSnapshots(images, snapshotMetaJson);

        Long resultId = wcagAnalysisService.analyze(
                githubId,
                repositoryUrl,
                branchName,
                snapshots
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200WCAG001",
                        "WCAG 분석 완료",
                        resultId
                )
        );
    }

    @Operation(
            summary = "저장된 WCAG 접근성 분석 결과 조회",
            description = "분석 실행 후 저장된 정적/AI WCAG 위반 위치를 조회합니다."
    )
    @GetMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<RealtimeAnalysisResponse>> getStoredAnalysis(
            @AuthenticationPrincipal Long githubId,
            @PathVariable Long repositoryId
    ) {
        RealtimeAnalysisResponse response = wcagAnalysisService.getStoredAnalysis(githubId, repositoryId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200WCAG002",
                        "WCAG 분석 결과 조회 완료",
                        response
                )
        );
    }

    private List<PageSnapshot> buildSnapshots(List<MultipartFile> images, String snapshotMetaJson) {
        List<SnapshotMetaDto> metaList;
        try {
            metaList = objectMapper.readValue(
                    snapshotMetaJson,
                    new TypeReference<List<SnapshotMetaDto>>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("snapshotMeta 형식이 올바르지 않습니다.", e);
        }

        if (images.size() != metaList.size()) {
            throw new IllegalArgumentException(
                    "images와 snapshotMeta의 개수가 일치하지 않습니다. images=%d, snapshotMeta=%d"
                            .formatted(images.size(), metaList.size())
            );
        }

        Map<String, MultipartFile> imageBySnapshotId = images.stream()
                .collect(Collectors.toMap(
                        MultipartFile::getOriginalFilename,
                        Function.identity()
                ));

        return metaList.stream()
                .map(meta -> {
                    MultipartFile image = imageBySnapshotId.get(meta.snapshotId());
                    if (image == null) {
                        throw new IllegalArgumentException(
                                "snapshotId에 매칭되는 이미지가 없습니다: " + meta.snapshotId()
                        );
                    }
                    return new PageSnapshot(meta.snapshotId(), image, meta.renderedFilePaths());
                })
                .toList();
    }
}
