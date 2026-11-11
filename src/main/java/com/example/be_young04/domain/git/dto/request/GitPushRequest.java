package com.example.be_young04.domain.git.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GitPushRequest(
        @NotBlank(message = "저장소 URL은 필수입니다.")
        String repositoryUrl,

        @NotBlank(message = "브랜치 이름은 필수입니다.")
        String branchName,

        String remote
) {
}
