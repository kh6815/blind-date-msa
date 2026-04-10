package com.project.blinddate.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLikeRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic likeNotificationTopic;
    private final ObjectMapper objectMapper;

    public void publishLikeNotification(Long likedUserId, Long likerUserId,
                                        String likerNickname, String likerProfileImageUrl) {
        try {
            LikeNotificationEvent event = LikeNotificationEvent.builder()
                    .likedUserId(likedUserId)
                    .likerUserId(likerUserId)
                    .likerNickname(likerNickname)
                    .likerProfileImageUrl(likerProfileImageUrl)
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(likeNotificationTopic.getTopic(), payload);
            log.info("Published like notification to Redis: likedUserId={}, likerUserId={}", likedUserId, likerUserId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize like notification event", e);
        }
    }

    @Getter
    @Builder
    public static class LikeNotificationEvent {
        private Long likedUserId;
        private Long likerUserId;
        private String likerNickname;
        private String likerProfileImageUrl;
    }
}
