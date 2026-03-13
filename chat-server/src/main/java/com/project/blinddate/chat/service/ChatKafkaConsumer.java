package com.project.blinddate.chat.service;

import com.project.blinddate.chat.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final ChatService chatService;

    @KafkaListener(topics = "chat-message-save", containerFactory = "chatMessageKafkaListenerContainerFactory")
    public void consume(ChatMessageEvent event) {
        log.info("Consumed message from Kafka: {}", event);
        chatService.saveMessage(event);
    }
}
