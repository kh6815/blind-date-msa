package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatUnreadBadgeEvent;
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
    private final ChannelTopic readEventTopic;
    private final ChannelTopic unreadBadgeTopic;
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

    /**
     * 읽음 이벤트를 Redis Pub/Sub을 통해 모든 서버 인스턴스에 발행합니다.
     * 각 서버 인스턴스는 자신에게 연결된 클라이언트들에게 WebSocket으로 전달합니다.
     *
     * @param readEvent 읽음 이벤트
     */
    public void publishReadEvent(ChatMessageReadEvent readEvent) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(readEvent);
            redisTemplate.convertAndSend(readEventTopic.getTopic(), jsonMessage);
            log.debug("Published read event to Redis topic {}: {}", readEventTopic.getTopic(), readEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize read event: {}", readEvent, e);
            throw new IllegalStateException("읽음 이벤트를 직렬화할 수 없습니다.", e);
        }
    }

    /**
     * 읽지 않은 메시지 뱃지 업데이트 이벤트를 특정 사용자에게 발행합니다.
     * Redis Pub/Sub을 통해 모든 서버 인스턴스에 전달되고, 해당 사용자가 연결된 서버에서 WebSocket으로 전달합니다.
     *
     * @param targetUserId 뱃지 업데이트를 받을 사용자 ID
     */
    public void publishUnreadBadgeUpdate(Long targetUserId) {
        try {
            ChatUnreadBadgeEvent event = ChatUnreadBadgeEvent.builder()
                    .targetUserId(targetUserId)
                    .build();
            String jsonMessage = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(unreadBadgeTopic.getTopic(), jsonMessage);
            log.debug("Published unread badge event to Redis topic {} for user: {}", unreadBadgeTopic.getTopic(), targetUserId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize unread badge event for user: {}", targetUserId, e);
            throw new IllegalStateException("뱃지 업데이트 이벤트를 직렬화할 수 없습니다.", e);
        }
    }
}
