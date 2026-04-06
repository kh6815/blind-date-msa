package com.project.blinddate.chat.service;

import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, ChatMessageEvent> chatMessageKafkaTemplate;
    private final KafkaTemplate<String, ChatMessageReadEvent> chatMessageReadKafkaTemplate;

    private static final String MESSAGE_TOPIC = "chat-message-save";
    private static final String READ_TOPIC = "chat-message-read";

    public void send(ChatMessageEvent event) {
        log.info("Sending message to Kafka topic: {}, payload: {}", MESSAGE_TOPIC, event);
        chatMessageKafkaTemplate.send(MESSAGE_TOPIC, event);
    }

    public void sendReadEvent(ChatMessageReadEvent event) {
        log.info("Sending read event to Kafka topic: {}, payload: {}", READ_TOPIC, event);
        chatMessageReadKafkaTemplate.send(READ_TOPIC, event);
    }
}
