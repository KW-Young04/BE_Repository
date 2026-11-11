package com.example.be_young04.domain.chat.dto;

import com.example.be_young04.domain.chat.entity.ChatMessage;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatDto {

    private ChatDto() {
    }

    public record SendRequest(
            @NotBlank @Size(max = 4000) String message,
            @NotBlank String repositoryUrl,
            @NotBlank String branchName,
            String activeFilePath,      // 선택: 사용자가 열어둔 파일
            Long selectedLocationId     // 선택: 사용자가 선택한 이슈 위치
    ) {
    }

    public record MessageItem(Long id, String senderType, String content, LocalDateTime createdAt) {
        public static MessageItem from(ChatMessage message) {
            return new MessageItem(message.getChatMessageId(), message.getSenderType(),
                    message.getContent(), message.getCreatedAt());
        }
    }

    public record FixProposal(String filePath, String before, String after, String description) {
    }

    public record SendResponse(
            MessageItem userMessage,
            MessageItem aiMessage,
            List<FixProposal> fixes,
            String fileContext          // FULL / SNIPPETS / NONE
    ) {
    }

    public record FixInput(@NotNull String before, @NotNull String after) {
    }

    public record ApplyRequest(
            @NotBlank String repositoryUrl,
            @NotBlank String branchName,
            @NotBlank String filePath,
            @NotEmpty List<@Valid FixInput> fixes
    ) {
    }

    public record ApplyResponse(
            String filePath,
            int appliedCount,
            List<String> failReasons,
            RealtimeAnalysisResponse remainingIssues,
            String content              // 적용 후 파일 전체 내용 (FE 에디터·미리보기 동기화용)
    ) {
    }
}