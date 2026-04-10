package com.project.blinddate.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_USER_INFO_UPDATED = "user-info-updated";

    public void sendUserRegistered(Long userId, String nickname, String profileImageUrl) {
        publish(UserInfoEvent.EventType.REGISTERED, userId, nickname, profileImageUrl);
    }

    public void sendUserInfoUpdated(Long userId, String nickname, String profileImageUrl) {
        publish(UserInfoEvent.EventType.UPDATED, userId, nickname, profileImageUrl);
    }

    public void sendUserDeleted(Long userId) {
        publish(UserInfoEvent.EventType.DELETED, userId, null, null);
    }

    private void publish(UserInfoEvent.EventType eventType, Long userId, String nickname, String profileImageUrl) {
        try {
            UserInfoEvent event = UserInfoEvent.builder()
                    .eventType(eventType)
                    .userId(userId)
                    .nickname(nickname)
                    .profileImageUrl(profileImageUrl)
                    .build();

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_USER_INFO_UPDATED, String.valueOf(userId), message);
            log.info("Published user info event: type={}, userId={}", eventType, userId);
        } catch (Exception e) {
            log.error("Failed to publish user info event: type={}, userId={}", eventType, userId, e);
        }
    }

    @Getter
    @Builder
    public static class UserInfoEvent {
        public enum EventType { REGISTERED, UPDATED, DELETED }

        private EventType eventType;
        private Long userId;
        private String nickname;
        private String profileImageUrl;
    }
}
