package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.domain.ChatRoom;
import com.project.blinddate.chat.repository.ChatRoomRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 채팅방 메타데이터 Redis 캐싱 서비스
 *
 * 목적:
 * 1. 채팅방 참여자 수를 빠르게 조회 (매번 DB 조회 방지)
 * 2. 읽음 처리 시 참여자 수 정보를 빠르게 제공
 * 3. 네트워크 부하 감소 (클라이언트가 참여자 수 전달 불필요)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final ObjectMapper objectMapper;

    @Value("${chatroom.metadata.key-prefix}")
    private String CHATROOM_METADATA_KEY_PREFIX;

    @Value("${chatroom.metadata.key-suffix}")
    private String METADATA_SUFFIX;

    @Value("${chatroom.metadata.cache-ttl-hours}")
    private long CACHE_TTL_HOURS;

    /**
     * 채팅방 참여자 수 조회 (캐시 우선)
     * @param roomId 채팅방 ID
     * @return 참여자 수
     */
    public int getParticipantCount(String roomId) {
        ChatRoomMetadata metadata = getChatRoomMetadata(roomId);
        return metadata != null ? metadata.getParticipantCount() : 0;
    }

    /**
     * 채팅방 메타데이터 조회 (캐시 우선, 없으면 DB에서 조회 후 캐싱)
     * @param roomId 채팅방 ID
     * @return 채팅방 메타데이터
     */
    public ChatRoomMetadata getChatRoomMetadata(String roomId) {
        String cacheKey = CHATROOM_METADATA_KEY_PREFIX + roomId + METADATA_SUFFIX;

        // 1. Redis 캐시에서 조회
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ChatRoomMetadata.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached chatroom metadata for room: {}", roomId, e);
            }
        }

        // 2. 캐시 미스 → DB에서 조회
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.warn("ChatRoom not found: {}", roomId);
            return null;
        }

        // 3. 메타데이터 생성
        List<Long> participantUserIds = room.getParticipantUserIds() != null
                ? room.getParticipantUserIds()
                : java.util.Collections.emptyList();

        ChatRoomMetadata metadata = new ChatRoomMetadata(
                roomId,
                participantUserIds.size(),
                participantUserIds
        );

        // 4. Redis에 캐싱
        try {
            String json = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(CACHE_TTL_HOURS));
        } catch (JsonProcessingException e) {
            log.error("Failed to cache chatroom metadata for room: {}", roomId, e);
        }

        return metadata;
    }

    /**
     * 채팅방 메타데이터 캐시 무효화
     * @param roomId 채팅방 ID
     */
    public void invalidateCache(String roomId) {
        String cacheKey = CHATROOM_METADATA_KEY_PREFIX + roomId + METADATA_SUFFIX;
        redisTemplate.delete(cacheKey);
        log.debug("Invalidated chatroom metadata cache for room: {}", roomId);
    }

    /**
     * 채팅방 메타데이터 VO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRoomMetadata {
        private String roomId;
        private int participantCount;
        private List<Long> participantUserIds;
    }
}