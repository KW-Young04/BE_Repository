package com.example.be_young04.domain.analysis.service;

import com.example.be_young04.domain.git.validator.GitRepositoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * before/after 문자열 패치 공통 로직 (AI 코드 수정, 챗봇 수정안에서 공유).
 * - before는 파일에 정확히 1회 등장해야 하고, 여러 수정안의 before 범위가 서로 겹치면 안 된다.
 */
@Component
@RequiredArgsConstructor
public class CodePatchSupport {

    private final GitRepositoryValidator gitRepositoryValidator;

    public String readFile(Path repositoryPath, String filePath) {
        gitRepositoryValidator.validateRepository(repositoryPath);
        String relativePath = gitRepositoryValidator.validateFilePath(repositoryPath, filePath);
        try {
            return Files.readString(repositoryPath.resolve(relativePath).normalize(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 읽을 수 없습니다: " + filePath);
        }
    }

    /**
     * 수정안 하나를 검증한다. 통과하면 null, 실패하면 사유를 반환한다.
     * 통과한 before 범위는 takenRanges에 추가되어 이후 수정안과의 겹침 검사에 쓰인다.
     */
    public String validate(String content, String before, String after, List<int[]> takenRanges) {
        if (before == null || before.isEmpty() || after == null) {
            return "수정안 형식이 올바르지 않습니다. 다시 시도해 주세요.";
        }
        if (before.equals(after)) {
            return "변경 내용이 없습니다.";
        }

        int count = countOccurrences(content, before);
        if (count == 0) {
            return "수정 대상 코드를 현재 파일에서 찾을 수 없습니다.";
        }
        if (count > 1) {
            return "수정 대상 코드가 파일에 여러 번 있어 위치를 특정할 수 없습니다.";
        }

        int start = content.indexOf(before);
        int end = start + before.length();
        if (takenRanges.stream().anyMatch(r -> start < r[1] && r[0] < end)) {
            return "다른 수정안과 수정 범위가 겹칩니다.";
        }
        takenRanges.add(new int[]{start, end});
        return null;
    }

    // 겹침 검사가 필요 없는 단건 검증용
    public String validate(String content, String before, String after) {
        return validate(content, before, after, new ArrayList<>());
    }

    public int countOccurrences(String text, String target) {
        if (target == null || target.isEmpty()) return 0;
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }
}