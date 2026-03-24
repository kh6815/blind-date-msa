package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic channelTopic;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(ChatMessageResponse message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", message, e);
            throw new IllegalStateException("메시지를 직렬화할 수 없습니다.", e);
        }
    }

    /**
     * 읽음 이벤트를 WebSocket을 통해 발행합니다.
     * @param readEvent 읽음 이벤트
     */
    public void publishReadEvent(ChatMessageReadEvent readEvent) {
        try {
            // WebSocket을 통해 읽음 이벤트 전송: /topic/chats/{roomId}/read
            String destination = "/topic/chats/" + readEvent.getRoomId() + "/read";
            messagingTemplate.convertAndSend(destination, readEvent);
            log.debug("Published read event to {}: {}", destination, readEvent);
        } catch (Exception e) {
            log.error("Failed to publish read event: {}", readEvent, e);
        }
    }
}
