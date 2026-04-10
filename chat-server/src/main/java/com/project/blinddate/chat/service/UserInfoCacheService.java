package com.project.blinddate.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.domain.ChatUserInfo;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.external.user_client.UserFeignClient;
import com.project.blinddate.chat.external.user_client.dto.UserFeignResponse;
import com.project.blinddate.chat.repository.ChatUserInfoRepository;
import com.project.blinddate.common.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoCacheService {

    private final ChatUserInfoRepository chatUserInfoRepository;
    private final ObjectMapper objectMapper;
    private final UserFeignClient userFeignClient;

    /**
     * MongoDB에서 유저 정보 조회.
     * 없으면 user-server Feign 호출 후 MongoDB에 저장(일회성 fallback).
     */
    public ChatUserInfoResponse getUserInfo(Long userId) {
        return chatUserInfoRepository.findById(userId)
                .filter(info -> !info.isDeleted())
                .map(info -> ChatUserInfoResponse.builder()
                        .userId(info.getUserId())
                        .nickname(info.getNickname())
                        .profileImageUrl(info.getProfileImageUrl())
                        .build())
                .orElseGet(() -> fetchAndSave(userId));
    }

    /**
     * user-info-updated Kafka 이벤트 수신 → MongoDB upsert
     */
    @KafkaListener(topics = "user-info-updated", groupId = "chat-server-user-info")
    public void handleUserInfoEvent(String message) {
        try {
            UserInfoEvent event = objectMapper.readValue(message, UserInfoEvent.class);
            if (event.userId == null) {
                log.warn("Received user-info event with null userId, skipping: {}", message);
                return;
            }

            switch (event.eventType) {
                case "REGISTERED", "UPDATED" -> upsert(event.userId, event.nickname, event.profileImageUrl);
                case "DELETED" -> chatUserInfoRepository.findById(event.userId)
                        .ifPresent(info -> {
                            info.markDeleted();
                            chatUserInfoRepository.save(info);
                            log.info("ChatUserInfo marked deleted: userId={}", event.userId);
                        });
                default -> log.warn("Unknown eventType: {}", event.eventType);
            }
        } catch (Exception e) {
            log.error("Error handling user-info event: {}", message, e);
        }
    }

    private void upsert(Long userId, String nickname, String profileImageUrl) {
        ChatUserInfo info = chatUserInfoRepository.findById(userId)
                .orElseGet(() -> ChatUserInfo.builder()
                        .userId(userId)
                        .deleted(false)
                        .updatedAt(Instant.now())
                        .build());
        info.update(nickname, profileImageUrl);
        chatUserInfoRepository.save(info);
        log.debug("ChatUserInfo upserted: userId={}", userId);
    }

    /**
     * MongoDB 미등록 유저에 대한 Feign fallback (일회성).
     * 이후 요청부터는 MongoDB에서 조회.
     */
    private ChatUserInfoResponse fetchAndSave(Long userId) {
        try {
            ResponseDto<UserFeignResponse> response = userFeignClient.getUserInfo(userId);
            if (response != null && response.getData() != null) {
                UserFeignResponse data = response.getData();
                upsert(data.getId(), data.getNickname(), data.getProfileImageUrl());
                return ChatUserInfoResponse.builder()
                        .userId(data.getId())
                        .nickname(data.getNickname() != null ? data.getNickname() : "알 수 없음")
                        .profileImageUrl(data.getProfileImageUrl())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info from user-server for userId={}", userId, e);
        }
        return defaultResponse(userId);
    }

    private ChatUserInfoResponse defaultResponse(Long userId) {
        return ChatUserInfoResponse.builder()
                .userId(userId)
                .nickname("상대방")
                .profileImageUrl(null)
                .build();
    }

    /** Kafka 메시지 역직렬화용 내부 DTO */
    private static class UserInfoEvent {
        public String eventType;
        public Long userId;
        public String nickname;
        public String profileImageUrl;
    }
}
