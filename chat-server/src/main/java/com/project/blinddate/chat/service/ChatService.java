package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.ChatMessage;
import com.project.blinddate.chat.domain.ChatRoom;
import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomCreateRequest;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.mapper.ChatMapper;
import com.project.blinddate.chat.repository.ChatMessageRepository;
import com.project.blinddate.chat.repository.ChatRoomRepository;
import com.project.blinddate.common.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;
    private final UserInfoCacheService userInfoCacheService;
    private final ChatKafkaProducer chatKafkaProducer;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;
    
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request) {
        // Check if room already exists
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByParticipants(request.getParticipantUserIds(), request.getParticipantUserIds().size());
        ChatRoom room;
        if (existingRoom.isPresent()) {
            room = existingRoom.get();
        } else {
            room = ChatRoom.builder()
                    .participantUserIds(request.getParticipantUserIds())
                    .createdAt(Instant.now())
                    .lastMessageAt(null)
                    .build();
            room = chatRoomRepository.save(room);
        }

        // UI 표시용 필드 채우기 (상대방 정보 등 - Kafka 기반)
        List<Long> participantUserIds = room.getParticipantUserIds() != null ? room.getParticipantUserIds() : java.util.Collections.emptyList();
        // myId, targetUserId 추출 (2명만 있다고 가정)
        Long myId = !participantUserIds.isEmpty() ? participantUserIds.get(0) : null;
        Long targetUserId = participantUserIds.size() > 1 ? participantUserIds.get(1) : null;

        String nickname = "상대방";
        String imageUrl = null;

        if (targetUserId != null) {
            ChatUserInfoResponse userInfo = userInfoCacheService.getUserInfo(targetUserId);
            if (userInfo != null) {
                nickname = userInfo.nickname != null ? userInfo.nickname : "상대방";
                imageUrl = userInfo.profileImageUrl;
            }
        }

        // 항상 builder로 반환 (UI 필드 포함)
        return ChatRoomResponse.builder()
                .id(room.getId())
                .participantUserIds(participantUserIds)
                .createdAt(room.getCreatedAt())
                .lastMessageAt(room.getLastMessageAt())
                .targetUserImageUrl(imageUrl)
                .targetUserNickname(nickname)
                .isTargetUserOnline(isUserOnline(targetUserId))
                .lastMessagePreview(getLastMessagePreview(room))
                .build();
    }

    @Transactional
    public ChatMessageResponse sendMessage(String roomId, Long senderUserId, String content, MessageType type) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        Instant now = Instant.now();

        ChatMessage message = ChatMessage.builder()
                .roomId(room.getId())
                .senderUserId(senderUserId)
                .content(content)
                .type(type)
                .sentAt(now)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        ChatRoom updatedRoom = ChatRoom.builder()
                .id(room.getId())
                .participantUserIds(room.getParticipantUserIds())
                .createdAt(room.getCreatedAt())
                .lastMessageAt(now)
                .build();
        chatRoomRepository.save(updatedRoom);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(saved.getId())
                .roomId(saved.getRoomId())
                .senderUserId(saved.getSenderUserId())
                .content(saved.getContent())
                .type(saved.getType() != null ? saved.getType().name() : MessageType.TEXT.name())
                .sentAt(saved.getSentAt())
                .build();
        chatKafkaProducer.publishChatMessage(event);

        return chatMapper.toMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentMessages(String roomId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId, pageable);

        return messagesPage.stream()
                .map(chatMapper::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomResponse> getRoomsByUser(Long userId, Pageable pageable) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipantUserIdsContains(userId);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), rooms.size());

        List<ChatRoomResponse> content = rooms.subList(start, end).stream().map(room -> {
                    List<Long> participantUserIds = room.getParticipantUserIds() != null ? room.getParticipantUserIds() : Collections.emptyList();
                    // 상대방 ID 추출 (본인 제외)
                    Long targetUserId = participantUserIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);

                    // 유저 서버에서 상대방 정보 조회 (Kafka 기반)
                    String nickname = "상대방";
                    String imageUrl = null;

                    if (targetUserId != null) {
                        ChatUserInfoResponse userInfo = userInfoCacheService.getUserInfo(targetUserId);
                        if (userInfo != null) {
                            nickname = userInfo.nickname != null ? userInfo.nickname : "상대방";
                            imageUrl = userInfo.profileImageUrl;
                        }
                    }

                    return ChatRoomResponse.builder()
                            .id(room.getId())
                            .participantUserIds(participantUserIds)
                            .createdAt(room.getCreatedAt())
                            .lastMessageAt(room.getLastMessageAt())
                            .targetUserImageUrl(imageUrl)
                            .targetUserNickname(nickname)
                            .lastMessagePreview(getLastMessagePreview(room))
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, rooms.size());
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(String roomId, Long currentUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        List<Long> participantUserIds = room.getParticipantUserIds() != null ? room.getParticipantUserIds() : Collections.emptyList();
        Long targetUserId = participantUserIds.stream().filter(id -> !id.equals(currentUserId)).findFirst().orElse(null);

        String nickname = "상대방";
        String imageUrl = null;

        if (targetUserId != null) {
            ChatUserInfoResponse userInfo = userInfoCacheService.getUserInfo(targetUserId);
            if (userInfo != null) {
                nickname = userInfo.nickname != null ? userInfo.nickname : "상대방";
                imageUrl = userInfo.profileImageUrl;
            }
        }

        return ChatRoomResponse.builder()
                .id(room.getId())
                .participantUserIds(participantUserIds)
                .createdAt(room.getCreatedAt())
                .lastMessageAt(room.getLastMessageAt())
                .targetUserImageUrl(imageUrl)
                .targetUserNickname(nickname)
                .isTargetUserOnline(isUserOnline(targetUserId))
                .build();
    }

    private String getLastMessagePreview(ChatRoom room) {
        String lastMessagePreview = "";
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderBySentAtAsc(room.getId());
        if (!messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            if (lastMessage.getType() == MessageType.IMAGE) {
                lastMessagePreview = "이미지";
            } else {
                lastMessagePreview = lastMessage.getContent();
            }
        }

        return lastMessagePreview;
    }

    /**
     * 사용자가 현재 온라인인지 확인합니다.
     */
    private boolean isUserOnline(Long userId) {
        if (userId == null) return false;

        String key = USER_PRESENCE_KEY_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }
}

