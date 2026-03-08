package com.project.blinddate.chat.service;

import com.project.blinddate.chat.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    private static final String TOPIC = "chat-message-save";

    public void send(ChatMessageEvent event) {
        log.info("Sending message to Kafka topic: {}, payload: {}", TOPIC, event);
        kafkaTemplate.send(TOPIC, event);
    }
}
