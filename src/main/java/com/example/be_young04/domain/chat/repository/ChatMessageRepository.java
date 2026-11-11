package com.example.be_young04.domain.chat.repository;

import com.example.be_young04.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRepositoryIdOrderByCreatedAtAsc(Long repositoryId);

    // AI 프롬프트에 넣을 최근 대화 (최신순으로 가져와 서비스에서 시간순으로 뒤집는다)
    List<ChatMessage> findTop10ByRepositoryIdOrderByCreatedAtDescChatMessageIdDesc(Long repositoryId);
}