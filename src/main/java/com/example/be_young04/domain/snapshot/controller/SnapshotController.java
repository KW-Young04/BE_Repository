package com.example.be_young04.domain.snapshot.controller;

import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import com.example.be_young04.domain.snapshot.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Snapshot", description = "스냅샷 API")
@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    @Operation(
            summary = "스냅샷 이미지 수신",
            description = "프론트엔드에서 캡처한 스냅샷 이미지를 수신합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SnapshotResponse> receive(
            @RequestParam("imageFile") MultipartFile imageFile
    ) {
        return ResponseEntity.ok(snapshotService.receive(imageFile));
    }
}