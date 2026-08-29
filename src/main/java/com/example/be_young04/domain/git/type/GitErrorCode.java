package com.example.be_young04.domain.git.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GitErrorCode {

    NOT_GIT_REPOSITORY(
            HttpStatus.BAD_REQUEST,
            "GIT_001",
            "Git 저장소가 아닙니다."
    ),

    NO_CHANGES(
            HttpStatus.BAD_REQUEST,
            "GIT_002",
            "커밋할 변경 사항이 없습니다."
    ),

    INVALID_FILE_PATH(
            HttpStatus.BAD_REQUEST,
            "GIT_003",
            "허용되지 않은 파일 경로입니다."
    ),

    MERGE_CONFLICT(
            HttpStatus.CONFLICT,
            "GIT_004",
            "충돌이 해결되지 않은 파일이 있습니다."
    ),

    COMMIT_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "GIT_005",
            "커밋 생성에 실패했습니다."
    ),

    PUSH_FAILED(
            HttpStatus.BAD_GATEWAY,
            "GIT_006",
            "원격 저장소 push에 실패했습니다."
    ),

    NON_FAST_FORWARD(
            HttpStatus.CONFLICT,
            "GIT_007",
            "원격 브랜치에 더 새로운 커밋이 있습니다."
    ),

    AUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "GIT_008",
            "GitHub 인증에 실패했습니다."
    ),

    COMMAND_TIMEOUT(
            HttpStatus.REQUEST_TIMEOUT,
            "GIT_009",
            "Git 명령 실행 시간이 초과되었습니다."
    ),

    COMMAND_EXECUTION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "GIT_010",
            "Git 명령을 실행할 수 없습니다."
    ),

    COMMAND_INTERRUPTED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "GIT_011",
            "Git 명령 실행이 중단되었습니다."
    ),

    INVALID_REPOSITORY_URL(
            HttpStatus.BAD_REQUEST,
            "GIT_012",
            "올바르지 않은 GitHub 저장소 URL입니다."
    ),

    INVALID_BRANCH(
            HttpStatus.BAD_REQUEST,
            "GIT_013",
            "올바르지 않은 Git 브랜치 이름입니다."
    ),

    CLONE_FAILED(
            HttpStatus.BAD_GATEWAY,
            "GIT_014",
            "GitHub 저장소 clone에 실패했습니다."
    ),

    WORKSPACE_PREPARATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "GIT_015",
            "Git workspace를 준비할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
