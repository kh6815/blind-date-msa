package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.ChatMessage;
import com.project.blinddate.chat.domain.ChatRoom;
import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomCreateRequest;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.mapper.ChatMapper;
import com.project.blinddate.chat.repository.ChatMessageRepository;
import com.project.blinddate.chat.repository.ChatRoomRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;
    private final UserInfoCacheService userInfoCacheService;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;
    
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void saveMessage(ChatMessageEvent event) {
        // 채팅방 존재 확인
        ChatRoom room = chatRoomRepository.findById(event.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        ChatMessage message = ChatMessage.builder()
                .id(event.getId())
                .roomId(event.getRoomId())
                .senderUserId(event.getSenderUserId())
                .content(event.getContent())
                .type(event.getType())
                .sentAt(event.getSentAt())
                .readBy(event.getReadBy() != null ? event.getReadBy() : new HashMap<>())
                .build();

        chatMessageRepository.save(message);

        // 채팅방 마지막 메시지 시간 업데이트
        ChatRoom updatedRoom = ChatRoom.builder()
                .id(room.getId())
                .participantUserIds(room.getParticipantUserIds())
                .createdAt(room.getCreatedAt())
                .lastMessageAt(event.getSentAt())
                .build();
        chatRoomRepository.save(updatedRoom);
    }

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

        // 발신자는 메시지 전송 시 자동으로 읽음 처리
        Map<Long, Instant> readBy = new HashMap<>();
        readBy.put(senderUserId, now);

        ChatMessage message = ChatMessage.builder()
                .roomId(room.getId())
                .senderUserId(senderUserId)
                .content(content)
                .type(type)
                .sentAt(now)
                .readBy(readBy)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        ChatRoom updatedRoom = ChatRoom.builder()
                .id(room.getId())
                .participantUserIds(room.getParticipantUserIds())
                .createdAt(room.getCreatedAt())
                .lastMessageAt(now)
                .build();
        chatRoomRepository.save(updatedRoom);

        return chatMapper.toMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentMessages(String roomId, int page, int size) {
        // 채팅방 정보 조회 (참여자 수 계산을 위함)
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        int totalParticipants = room.getParticipantUserIds() != null ? room.getParticipantUserIds().size() : 0;

        PageRequest pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId, pageable);

        return messagesPage.stream()
                .map(message -> toMessageResponseWithUnreadCount(message, totalParticipants))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomResponse> getRoomsByUser(Long userId, Pageable pageable) {
        Page<ChatRoom> roomPage = chatRoomRepository.findByParticipantUserIdsContains(userId, pageable);

        List<ChatRoomResponse> content = roomPage.getContent().stream().map(room -> {
                    List<Long> participantUserIds = room.getParticipantUserIds() != null ? room.getParticipantUserIds() : Collections.emptyList();
                    Long targetUserId = participantUserIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);

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

        return new PageImpl<>(content, pageable, roomPage.getTotalElements());
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
        return chatMessageRepository.findTop1ByRoomIdOrderBySentAtDesc(room.getId())
                .map(msg -> msg.getType() == MessageType.IMAGE ? "이미지" : msg.getContent())
                .orElse("");
    }

    /**
     * 특정 채팅방의 메시지들을 읽음 처리합니다.
     * @param roomId 채팅방 ID
     * @param userId 읽음 처리할 사용자 ID
     * @return 읽음 처리된 메시지 ID 목록
     */
    @Transactional
    public List<String> markMessagesAsRead(String roomId, Long userId) {
        // 채팅방 존재 확인
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        Instant now = Instant.now();
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);

        // 읽지 않은 메시지만 필터링하여 읽음 처리
        List<String> updatedMessageIds = messages.stream()
                .filter(msg -> !msg.isReadBy(userId))
                .peek(msg -> msg.markAsReadBy(userId, now))
                .map(ChatMessage::getId)
                .collect(Collectors.toList());

        // 변경된 메시지들 일괄 저장
        if (!updatedMessageIds.isEmpty()) {
            chatMessageRepository.saveAll(
                    messages.stream()
                            .filter(msg -> updatedMessageIds.contains(msg.getId()))
                            .collect(Collectors.toList())
            );
        }

        return updatedMessageIds;
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 수를 조회합니다.
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 수
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(String roomId, Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);

        return messages.stream()
                .filter(msg -> !msg.isReadBy(userId))
                .count();
    }

    /**
     * 메시지를 응답 DTO로 변환하면서 unreadCount를 계산합니다.
     * @param message 메시지
     * @param totalParticipants 채팅방 전체 참여자 수
     * @return 메시지 응답 DTO
     */
    private ChatMessageResponse toMessageResponseWithUnreadCount(ChatMessage message, int totalParticipants) {
        int readCount = message.getReadBy() != null ? message.getReadBy().size() : 0;
        int unreadCount = Math.max(0, totalParticipants - readCount);

        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .senderUserId(message.getSenderUserId())
                .content(message.getContent())
                .type(message.getType())
                .sentAt(message.getSentAt())
                .readBy(message.getReadBy())
                .unreadCount(unreadCount)
                .build();
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

