package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatKafkaProducerTest {

    @InjectMocks
    private ChatKafkaProducer chatKafkaProducer;

    @Mock
    private KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;

    @Test
    @DisplayName("채팅 메시지 이벤트를 Kafka에 전송한다")
    void send_success() {
        ChatMessageEvent event = ChatMessageEvent.builder()
                .id("msg1")
                .roomId("room1")
                .senderUserId(1L)
                .content("hello")
                .type(MessageType.TEXT)
                .sentAt(Instant.now())
                .build();

        chatKafkaProducer.send(event);

        verify(kafkaTemplate, times(1))
                .send(eq("chat-message-save"), eq(event));
    }
}

