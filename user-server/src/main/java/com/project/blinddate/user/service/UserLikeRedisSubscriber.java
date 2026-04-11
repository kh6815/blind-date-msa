package com.project.blinddate.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.user.service.UserLikeRedisPublisher.LikeNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLikeRedisSubscriber {

    private final ObjectMapper objectMapper;
    private final UserSseService userSseService;

    /**
     * Redis Pub/Sub → 모든 user-server 인스턴스에서 호출됨
     * 해당 인스턴스에 SSE 연결된 유저에게만 이벤트 전송
     */
    public void onLikeNotification(String message) {
        try {
            LikeNotificationEvent event = objectMapper.readValue(message, LikeNotificationEvent.class);
            log.info("Received like notification from Redis: likedUserId={}", event.getLikedUserId());

            String payload = objectMapper.writeValueAsString(event);
            userSseService.sendLikeNotification(event.getLikedUserId(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize like notification event: {}", message, e);
        }
    }
}
