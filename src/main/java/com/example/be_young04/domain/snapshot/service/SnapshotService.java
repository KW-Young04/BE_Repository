package com.example.be_young04.domain.snapshot.service;

import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class SnapshotService {

    public SnapshotResponse receive(MultipartFile imageFile) {
        validateImageFile(imageFile);

        try {
            byte[] imageBytes = imageFile.getBytes();

            return SnapshotResponse.builder()
                    .imageBytes(imageBytes)
                    .width(1440)
                    .height(900)
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException("스냅샷 이미지 처리에 실패했습니다.", e);
        }
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("스냅샷 이미지가 비어 있습니다.");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
    }
}