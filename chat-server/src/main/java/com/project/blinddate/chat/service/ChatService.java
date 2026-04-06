package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.ChatMessage;
import com.project.blinddate.chat.domain.ChatRoom;
import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomCreateRequest;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.mapper.ChatMapper;
import com.project.blinddate.chat.repository.ChatMessageRepository;
import com.project.blinddate.chat.repository.ChatRoomRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;
    private final UserInfoCacheService userInfoCacheService;
    private final ChatRoomCacheService chatRoomCacheService;

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

    /**
     * 채팅방 참여자 목록을 조회합니다 (발신자 제외).
     *
     * @param roomId 채팅방 ID
     * @param excludeUserId 제외할 사용자 ID (일반적으로 발신자)
     * @return 참여자 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getRoomParticipantsExcept(String roomId, Long excludeUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null || room.getParticipantUserIds() == null) {
            return Collections.emptyList();
        }

        return room.getParticipantUserIds().stream()
                .filter(userId -> !userId.equals(excludeUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request) {
        // null 값 필터링 및 유효성 검사
        List<Long> validParticipants = request.getParticipantUserIds().stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (validParticipants.isEmpty()) {
            throw new IllegalArgumentException("유효한 참여자가 없습니다.");
        }

        // Check if room already exists
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByParticipants(validParticipants, validParticipants.size());
        ChatRoom room;
        if (existingRoom.isPresent()) {
            room = existingRoom.get();
        } else {
            room = ChatRoom.builder()
                    .participantUserIds(validParticipants)
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
        // 채팅방 참여자 수 조회 (캐시 우선)
        int totalParticipants = chatRoomCacheService.getParticipantCount(roomId);
        if (totalParticipants == 0) {
            // 캐시 미스이고 채팅방이 존재하지 않는 경우 예외 발생
            chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        }

        PageRequest pageable = PageRequest.of(page, size);
        // 최근 메시지부터 조회 (Desc 정렬)
        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);

        List<ChatMessageResponse> messages = messagesPage.stream()
                .map(message -> toMessageResponseWithUnreadCount(message, totalParticipants))
                .collect(Collectors.toList());

        // 역순으로 뒤집어서 오래된 순서로 반환 (프론트엔드에서 그대로 표시)
        Collections.reverse(messages);
        return messages;
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

                    // 읽지 않은 메시지 수 계산
                    Long unreadCount = getUnreadCount(room.getId(), userId);

                    return ChatRoomResponse.builder()
                            .id(room.getId())
                            .participantUserIds(participantUserIds)
                            .createdAt(room.getCreatedAt())
                            .lastMessageAt(room.getLastMessageAt())
                            .targetUserImageUrl(imageUrl)
                            .targetUserNickname(nickname)
                            .lastMessagePreview(getLastMessagePreview(room))
                            .unreadMessageCount(unreadCount)
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
     * Timestamp 기반으로 메시지들을 읽음 처리합니다 (동기 처리 + 경량 이벤트 생성).
     * readAt 시간 이전의 모든 읽지 않은 메시지를 읽음 처리합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 읽음 처리할 사용자 ID
     * @param readAt 읽은 시간 (이 시간 이전의 모든 메시지가 읽음 처리됨)
     * @return 읽음 처리된 메시지 개수
     */
    @Transactional
    public int markMessagesAsReadByTimestamp(String roomId, Long userId, Instant readAt) {
        // 채팅방 존재 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);

        // readAt 시간 이전의 읽지 않은 메시지만 필터링하여 읽음 처리
        List<ChatMessage> updatedMessages = messages.stream()
                .filter(msg -> !msg.isReadBy(userId))  // 아직 안 읽은 메시지
                .filter(msg -> !msg.getSentAt().isAfter(readAt))  // readAt 이전에 전송된 메시지
                .peek(msg -> msg.markAsReadBy(userId, readAt))
                .collect(Collectors.toList());

        // 변경된 메시지들 일괄 저장
        if (!updatedMessages.isEmpty()) {
            chatMessageRepository.saveAll(updatedMessages);
            log.info("Marked {} messages as read for user {} in room {} (readAt: {})",
                    updatedMessages.size(), userId, roomId, readAt);
        }

        return updatedMessages.size();
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 수를 조회합니다.
     * 성능 최적화: MongoDB 쿼리 레벨에서 읽지 않은 메시지만 최대 11개 조회
     * 10개 이상인 경우 10을 반환 (UI에서 "10+" 표시용)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 수 (최대 10)
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(String roomId, Long userId) {
        // MongoDB 쿼리로 읽지 않은 메시지만 최대 11개 조회
        int limitMessage = 11;
        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesByUserId(roomId, userId, limitMessage);

        int count = unreadMessages.size();

        // 10개 이상이면 10 반환 (UI에서 "10+" 처리)
        return (long) Math.min(count, 10);
    }

    /**
     * 사용자에게 읽지 않은 메시지가 하나라도 있는지 확인합니다.
     * 성능 최적화: MongoDB 쿼리로 첫 번째 읽지 않은 메시지만 조회 (limit 1)
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasUnreadMessages(Long userId) {
        // 사용자가 참여한 모든 채팅방 조회
        Page<ChatRoom> rooms = chatRoomRepository.findByParticipantUserIdsContains(
            userId,
            PageRequest.of(0, Integer.MAX_VALUE)
        );

        // 각 채팅방에서 읽지 않은 메시지가 하나라도 있는지 확인
        for (ChatRoom room : rooms.getContent()) {
            List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesByUserId(
                room.getId(),
                userId,
                1  // 1개만 조회
            );
            if (!unreadMessages.isEmpty()) {
                return true;
            }
        }

        return false;
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

    /**
     * Kafka를 통해 전달된 읽음 이벤트를 처리하여 DB에 반영합니다 (Timestamp 기반).
     * 주의: 이 메서드는 멱등성을 보장합니다.
     *
     * 목적:
     * 1. 백업/감사 로그: Kafka 토픽에 읽음 이벤트가 영구적으로 기록됨
     * 2. 재처리 지원: markMessagesAsReadByTimestamp 실패 시 Kafka를 통해 재처리
     * 3. 분산 환경 일관성: 여러 서버 인스턴스 간 데이터 동기화
     *
     * @param event 읽음 이벤트
     */
    @Transactional
    public void processReadEvent(ChatMessageReadEvent event) {
        String roomId = event.getRoomId();
        Long userId = event.getUserId();
        Instant readAt = event.getReadAt();

        log.info("Processing read event from Kafka for room: {}, user: {}, readAt: {}", roomId, userId, readAt);

        // 채팅방 존재 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);

        // readAt 시간 이전의 읽지 않은 메시지만 필터링하여 읽음 처리 (멱등성 보장)
        List<ChatMessage> updatedMessages = messages.stream()
                .filter(msg -> !msg.isReadBy(userId))  // 아직 안 읽은 메시지
                .filter(msg -> !msg.getSentAt().isAfter(readAt))  // readAt 이전 메시지
                .peek(msg -> msg.markAsReadBy(userId, readAt))
                .collect(Collectors.toList());

        // 변경된 메시지들 일괄 저장
        if (!updatedMessages.isEmpty()) {
            chatMessageRepository.saveAll(updatedMessages);
            log.info("Updated {} messages as read for user: {} via Kafka", updatedMessages.size(), userId);
        } else {
            log.debug("No messages to update for user: {} in room: {} (already processed)", userId, roomId);
        }
    }
}

