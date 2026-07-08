package com.example.be_young04.domain.chat.repository;

import com.example.be_young04.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRepositoryIdOrderByCreatedAtAsc(Long repositoryId);
}