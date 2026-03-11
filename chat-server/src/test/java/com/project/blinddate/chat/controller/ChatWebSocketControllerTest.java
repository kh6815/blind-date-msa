package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageSendRequest;
import com.project.blinddate.chat.service.ChatKafkaProducer;
import com.project.blinddate.chat.service.ChatRedisPublisher;
import com.project.blinddate.chat.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatRedisPublisher chatRedisPublisher;

    @Mock
    private ChatKafkaProducer chatKafkaProducer;

    @Test
    @DisplayName("WebSocket 메시지 수신 시 Redis 발행 및 Kafka 이벤트 발행을 수행한다")
    void sendMessage_publishToRedisAndKafka() {
        String roomId = "room1";
        ChatMessageSendRequest request = ChatMessageSendRequest.builder()
                .senderUserId(1L)
                .content("hello")
                .type(MessageType.TEXT)
                .build();

        chatWebSocketController.sendMessage(roomId, request);

        ArgumentCaptor<ChatMessageResponse> redisCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        ArgumentCaptor<ChatMessageEvent> kafkaCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);

        verify(chatRedisPublisher, times(1)).publish(redisCaptor.capture());
        verify(chatKafkaProducer, times(1)).send(kafkaCaptor.capture());

        ChatMessageResponse redisMessage = redisCaptor.getValue();
        ChatMessageEvent kafkaEvent = kafkaCaptor.getValue();

        assertThat(redisMessage.getRoomId()).isEqualTo(roomId);
        assertThat(redisMessage.getSenderUserId()).isEqualTo(1L);
        assertThat(redisMessage.getContent()).isEqualTo("hello");
        assertThat(redisMessage.getType()).isEqualTo(MessageType.TEXT);
        assertThat(redisMessage.getSentAt()).isNotNull();

        assertThat(kafkaEvent.getRoomId()).isEqualTo(roomId);
        assertThat(kafkaEvent.getSenderUserId()).isEqualTo(1L);
        assertThat(kafkaEvent.getContent()).isEqualTo("hello");
        assertThat(kafkaEvent.getType()).isEqualTo(MessageType.TEXT);
        assertThat(kafkaEvent.getSentAt()).isNotNull();
    }
}

