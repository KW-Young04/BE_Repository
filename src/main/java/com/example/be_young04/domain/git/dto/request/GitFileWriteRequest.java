package com.example.be_young04.domain.git.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GitFileWriteRequest(
        @NotBlank(message = "저장소 URL은 필수입니다.")
        String repositoryUrl,

        @NotBlank(message = "브랜치 이름은 필수입니다.")
        String branchName,

        @NotBlank(message = "파일 경로는 필수입니다.")
        String path,

        @NotNull(message = "파일 내용은 필수입니다.")
        String content
) {
}
