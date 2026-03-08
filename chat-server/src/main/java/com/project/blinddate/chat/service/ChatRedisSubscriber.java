package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void onMessage(String message) {
        try {
            ChatMessageResponse response = objectMapper.readValue(message, ChatMessageResponse.class);
            messagingTemplate.convertAndSend("/topic/chats/" + response.getRoomId(), response);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message: {}", message, e);
        }
    }
}
