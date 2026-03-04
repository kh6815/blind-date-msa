package com.project.blinddate.chat.service;

import com.project.blinddate.common.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;

    @Value("${chat.kafka.topics.message}")
    private String messageTopic;

    public void publishChatMessage(ChatMessageEvent event) {
        kafkaTemplate.send(messageTopic, event.getRoomId(), event);
    }
}


