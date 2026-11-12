package com.example.be_young04.domain.chat.service;

import com.example.be_young04.domain.chat.entity.ChatMessage;
import com.example.be_young04.domain.chat.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatHistoryTest {
    private final ChatMessageRepository repository = mock(ChatMessageRepository.class);
    private final ChatService service = new ChatService(null, null, repository, null, null, null, null, null);

    @Test void newConversationDoesNotReadStoredHistory() {
        assertTrue(service.recentHistory(7L, List.of()).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test void currentConversationIsScopedToRepositoryAndExplicitIds() {
        var ids = List.of(11L, 12L);
        var messages = List.of(ChatMessage.builder().repositoryId(7L).content("current").build());
        when(repository.findByRepositoryIdAndChatMessageIdInOrderByCreatedAtAscChatMessageIdAsc(7L, ids))
                .thenReturn(messages);
        assertEquals(messages, service.recentHistory(7L, ids));
        verify(repository).findByRepositoryIdAndChatMessageIdInOrderByCreatedAtAscChatMessageIdAsc(7L, ids);
        verifyNoMoreInteractions(repository);
    }

    @Test void olderClientsKeepChronologicalHistory() {
        var older = ChatMessage.builder().content("older").build();
        var newer = ChatMessage.builder().content("newer").build();
        when(repository.findTop10ByRepositoryIdOrderByCreatedAtDescChatMessageIdDesc(7L))
                .thenReturn(List.of(newer, older));
        assertEquals(List.of(older, newer), service.recentHistory(7L, null));
    }
}
