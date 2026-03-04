package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.common.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "${chat.kafka.topics.message}", containerFactory = "chatMessageKafkaListenerContainerFactory")
    public void consumeChatMessage(ChatMessageEvent event) {
        log.info("Consumed message: {}", event);

        // Convert event to response DTO if needed, or send event directly
        // Usually we send a specific response format to the client
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(event.getMessageId())
                .roomId(event.getRoomId())
                .senderUserId(event.getSenderUserId())
                .content(event.getContent())
                .type(MessageType.valueOf(event.getType() != null ? event.getType() : "TEXT"))
                .sentAt(event.getSentAt())
                .build();

        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/chats/" + event.getRoomId(), response);
    }
}
