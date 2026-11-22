package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomUpdateEvent;
import com.project.blinddate.chat.dto.ChatUnreadBadgeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final SseService sseService;

    /**
     * 일반 채팅 메시지 처리 (Redis Pub/Sub → WebSocket)
     */
    public void onMessage(String message) {
        try {
            ChatMessageResponse response = objectMapper.readValue(message, ChatMessageResponse.class);
            messagingTemplate.convertAndSend("/topic/chats/" + response.getRoomId(), response);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message: {}", message, e);
        }
    }

    /**
     * 읽음 이벤트 처리 (Redis Pub/Sub → WebSocket)
     * 모든 서버 인스턴스가 읽음 이벤트를 받아서 자신에게 연결된 클라이언트들에게 전달
     */
    public void onReadEvent(String message) {
        try {
            ChatMessageReadEvent readEvent = objectMapper.readValue(message, ChatMessageReadEvent.class);
            String destination = "/topic/chats/" + readEvent.getRoomId() + "/read";
            messagingTemplate.convertAndSend(destination, readEvent);
            log.debug("Forwarded read event to WebSocket: {}", destination);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize read event: {}", message, e);
        }
    }

    /**
     * 읽지 않은 메시지 뱃지 업데이트 이벤트 처리 (Redis Pub/Sub → SSE)
     * 단일 또는 다중 사용자에게 뱃지 업데이트를 SSE로 전송합니다.
     */
    public void onUnreadBadge(String message) {
        try {
            ChatUnreadBadgeEvent badgeEvent = objectMapper.readValue(message, ChatUnreadBadgeEvent.class);

            // 배치 처리 우선 (targetUserIds가 있으면 배치로 처리)
            if (badgeEvent.getTargetUserIds() != null && !badgeEvent.getTargetUserIds().isEmpty()) {
                log.info("📩 [Badge SSE] Received batch from Redis - {} users", badgeEvent.getTargetUserIds().size());
                // SSE를 통해 뱃지 업데이트 전송 (연결된 서버만 전송)
                badgeEvent.getTargetUserIds().forEach(userId -> {
                    sseService.sendBadgeUpdateMessage(userId);
                });
            } else if (badgeEvent.getTargetUserId() != null) {
                // 단일 사용자 처리 (하위 호환성)
                log.info("📩 [Badge SSE] Received from Redis - targetUserId: {}", badgeEvent.getTargetUserId());
                sseService.sendBadgeUpdateMessage(badgeEvent.getTargetUserId());
            }
        } catch (JsonProcessingException e) {
            log.error("❌ [Badge SSE] Failed to deserialize unread badge event: {}", message, e);
        }
    }

    /**
     * 채팅방 업데이트 이벤트 처리 (Redis Pub/Sub → WebSocket)
     * 모든 서버 인스턴스가 채팅방 업데이트 이벤트를 받아서 자신에게 연결된 참여자들에게 WebSocket으로 전달
     * 채팅방 단위로 1회만 발행되므로 각 서버에서 참여자별로 전송
     */
    public void onChatRoomUpdate(String message) {
        try {
            ChatRoomUpdateEvent event = objectMapper.readValue(message, ChatRoomUpdateEvent.class);

            if (event.getUpdateType() == ChatRoomUpdateEvent.UpdateType.CREATED && event.getChatRoom() != null) {
                // 생성 이벤트: 채팅방의 모든 참여자에게 사용자별 토픽으로 전송
                event.getChatRoom().getParticipantUserIds().forEach(userId -> {
                    String destination = "/topic/users/" + userId + "/chat-updates";
                    messagingTemplate.convertAndSend(destination, event);
                    log.debug("Forwarded chatroom CREATED event to WebSocket: {}", destination);
                });
            } else if (event.getUpdateType() == ChatRoomUpdateEvent.UpdateType.UPDATED && event.getRoomId() != null) {
                // 업데이트 이벤트: 채팅방 참여자에게 전송 (participantUserIds가 있어야 함)
                if (event.getChatRoom() != null && event.getChatRoom().getParticipantUserIds() != null) {
                    event.getChatRoom().getParticipantUserIds().forEach(userId -> {
                        String destination = "/topic/users/" + userId + "/chat-updates";
                        messagingTemplate.convertAndSend(destination, event);
                        log.debug("Forwarded chatroom UPDATED event to WebSocket: {}", destination);
                    });
                }
            } else if (event.getUpdateType() == ChatRoomUpdateEvent.UpdateType.DELETED && event.getRoomId() != null) {
                // 삭제 이벤트: 채팅방 참여자에게 전송 (participantUserIds가 있어야 함)
                if (event.getChatRoom() != null && event.getChatRoom().getParticipantUserIds() != null) {
                    event.getChatRoom().getParticipantUserIds().forEach(userId -> {
                        String destination = "/topic/users/" + userId + "/chat-updates";
                        messagingTemplate.convertAndSend(destination, event);
                        log.debug("Forwarded chatroom DELETED event to WebSocket: {}", destination);
                    });
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chatroom update event: {}", message, e);
        }
    }
}
