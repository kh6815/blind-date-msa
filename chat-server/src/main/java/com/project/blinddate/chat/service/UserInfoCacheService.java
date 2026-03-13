package com.project.blinddate.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.external.user_client.UserFeignClient;
import com.project.blinddate.chat.external.user_client.dto.UserFeignResponse;
import com.project.blinddate.common.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserFeignClient userFeignClient;

    private static final String USER_INFO_CACHE_PREFIX = "user:info:";
    private static final long CACHE_TTL_HOURS = 24;

    /**
     * Redis 캐시에서 사용자 정보 조회 (동기)
     * 캐시 미스 시 user-server API 호출하여 조회 및 캐시 갱신
     */
    public ChatUserInfoResponse getUserInfo(Long userId) {
        try {
            String cacheKey = USER_INFO_CACHE_PREFIX + userId;
            String cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                return objectMapper.readValue(cached, ChatUserInfoResponse.class);
            }
            
            // 캐시 미스 시 user-server 호출
            try {
                ResponseDto<UserFeignResponse> response = userFeignClient.getUserInfo(userId);
                
                if (response != null && response.getData() != null) {
                    UserFeignResponse data = response.getData();
                    
                    ChatUserInfoResponse userInfo = ChatUserInfoResponse.builder()
                            .userId(data.getId())
                            .nickname(data.getNickname() != null ? data.getNickname() : "알 수 없음")
                            .profileImageUrl(data.getProfileImageUrl())
                            .build();
                            
                    // 캐시 업데이트
                    String cacheValue = objectMapper.writeValueAsString(userInfo);
                    redisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL_HOURS, TimeUnit.HOURS);
                    
                    return userInfo;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user info from user-server for userId: {}", userId, e);
            }
            
            // 캐시 미스 & API 실패 시 기본값 반환
            return ChatUserInfoResponse.builder()
                    .userId(userId)
                    .nickname("상대방")
                    .profileImageUrl(null)
                    .build();
                    
        } catch (Exception e) {
            log.warn("Error getting user info for userId: {}", userId, e);
            return ChatUserInfoResponse.builder()
                    .userId(userId)
                    .nickname("상대방")
                    .profileImageUrl(null)
                    .build();
        }
    }

    /**
     * user-server의 사용자 정보 변경 이벤트 구독
     * Kafka 이벤트로 Redis 캐시 업데이트
     */
    @KafkaListener(topics = "user-info-updated", groupId = "chat-server-user-cache")
    public void handleUserInfoUpdated(String message) {
        try {
            ChatUserInfoResponse userInfo = objectMapper.readValue(message, ChatUserInfoResponse.class);
            if (userInfo.userId == null) {
                log.warn("Received user-info-updated event with null userId, skipping: {}", message);
                return;
            }
            String cacheKey = USER_INFO_CACHE_PREFIX + userInfo.userId;
            String cacheValue = objectMapper.writeValueAsString(userInfo);

            redisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.debug("User info cache updated: userId={}", userInfo.userId);
        } catch (Exception e) {
            log.error("Error handling user info updated event: {}", message, e);
        }
    }
}
