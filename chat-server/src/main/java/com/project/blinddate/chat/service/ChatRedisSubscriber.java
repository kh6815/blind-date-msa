package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
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
    private final ChatBadgeSseService chatBadgeSseService;

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
     * 특정 사용자에게 뱃지 업데이트를 SSE로 전송합니다.
     */
    public void onUnreadBadge(String message) {
        try {
            ChatUnreadBadgeEvent badgeEvent = objectMapper.readValue(message, ChatUnreadBadgeEvent.class);
            log.info("📩 [Badge SSE] Received from Redis - targetUserId: {}", badgeEvent.getTargetUserId());

            // SSE를 통해 뱃지 업데이트 전송 (연결된 서버만 전송)
            chatBadgeSseService.notifyBadgeUpdate(badgeEvent.getTargetUserId());
        } catch (JsonProcessingException e) {
            log.error("❌ [Badge SSE] Failed to deserialize unread badge event: {}", message, e);
        }
    }
}
