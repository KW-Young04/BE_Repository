package com.example.be_young04.domain.chat.controller;

import com.example.be_young04.domain.chat.dto.ChatDto;
import com.example.be_young04.domain.chat.service.ChatService;
import com.example.be_young04.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Chat", description = "AI 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "대화 기록 조회", description = "저장소의 챗봇 대화 기록을 시간순으로 조회합니다.")
    @GetMapping("/{repositoryId}/messages")
    public ResponseEntity<ApiResponse<List<ChatDto.MessageItem>>> getMessages(
            @AuthenticationPrincipal Long githubId,
            @PathVariable Long repositoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success("200CHAT001", "대화 기록 조회 완료",
                chatService.getMessages(githubId, repositoryId)));
    }

    @Operation(summary = "챗봇 질문", description = "분석 결과·열린 파일·이전 대화를 맥락으로 답변하고, 요청 시 코드 수정안을 제안합니다.")
    @PostMapping("/{repositoryId}/messages")
    public ResponseEntity<ApiResponse<ChatDto.SendResponse>> send(
            @AuthenticationPrincipal Long githubId,
            @PathVariable Long repositoryId,
            @Valid @RequestBody ChatDto.SendRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("200CHAT002", "AI 응답 완료",
                chatService.send(githubId, repositoryId, request)));
    }

    @Operation(summary = "챗봇 수정안 적용", description = "채팅에서 제안된 before/after 수정안을 작업 파일에 반영하고 정적 룰로 재검사합니다.")
    @PostMapping("/{repositoryId}/fixes/apply")
    public ResponseEntity<ApiResponse<ChatDto.ApplyResponse>> applyFixes(
            @AuthenticationPrincipal Long githubId,
            @PathVariable Long repositoryId,
            @Valid @RequestBody ChatDto.ApplyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("200CHAT003", "수정안 적용 완료",
                chatService.applyFixes(githubId, repositoryId, request)));
    }

    // 실패 사유(예: AI 호출 실패)를 FE가 response.data.message로 그대로 표시할 수 있게 ApiResponse 형식으로 내려준다.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.fail("CHAT_" + e.getStatusCode().value(), e.getReason()));
    }
}