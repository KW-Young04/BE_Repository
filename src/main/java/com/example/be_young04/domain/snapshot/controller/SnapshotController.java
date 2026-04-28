package com.example.be_young04.domain.snapshot.controller;

import com.example.be_young04.domain.snapshot.dto.SnapshotRequest;
import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import com.example.be_young04.domain.snapshot.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    @PostMapping
    public SnapshotResponse capture(@RequestBody SnapshotRequest request) {
        return snapshotService.capture(request.getDeploymentUrl());
    }
}