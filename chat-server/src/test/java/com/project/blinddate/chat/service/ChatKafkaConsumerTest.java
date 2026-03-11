package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatKafkaConsumerTest {

    @InjectMocks
    private ChatKafkaConsumer chatKafkaConsumer;

    @Mock
    private ChatService chatService;

    @Test
    @DisplayName("Kafka에서 수신한 채팅 메시지 이벤트를 ChatService로 위임한다")
    void consume_success() {
        ChatMessageEvent event = ChatMessageEvent.builder()
                .id("msg1")
                .roomId("room1")
                .senderUserId(1L)
                .content("hello")
                .type(MessageType.TEXT)
                .sentAt(Instant.now())
                .build();

        chatKafkaConsumer.consume(event);

        verify(chatService, times(1)).saveMessage(event);
    }
}

