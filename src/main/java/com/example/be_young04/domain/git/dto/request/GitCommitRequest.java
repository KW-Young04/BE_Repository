package com.example.be_young04.domain.git.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GitCommitRequest(

        @NotBlank(message = "저장소 URL은 필수입니다.")
        String repositoryUrl,

        @NotBlank(message = "브랜치 이름은 필수입니다.")
        String branchName,

        @NotBlank(message = "커밋 메시지는 필수입니다.")
        @Size(max = 200, message = "커밋 메시지는 200자 이하여야 합니다.")
        String message,

        @NotEmpty(message = "커밋할 파일을 하나 이상 선택해야 합니다.")
        List<@NotBlank String> files
) {
}
