package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic channelTopic;
    private final ObjectMapper objectMapper;

    public void publish(ChatMessageResponse message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", message, e);
            throw new IllegalStateException("메시지를 직렬화할 수 없습니다.", e);
        }
    }
}
