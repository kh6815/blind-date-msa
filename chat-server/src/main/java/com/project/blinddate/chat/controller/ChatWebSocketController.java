package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatMessageSendRequest;
import com.project.blinddate.chat.service.ChatKafkaProducer;
import com.project.blinddate.chat.service.ChatRedisPublisher;
import com.project.blinddate.chat.service.ChatRoomCacheService;
import com.project.blinddate.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// /pub
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final ChatKafkaProducer chatKafkaProducer;
    private final ChatRoomCacheService chatRoomCacheService;

    @MessageMapping("/chats/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageSendRequest request
    ) {
        // chatService.sendMessage(roomId, request.getSenderUserId(), request.getContent(), request.getType());

        // ID 생성
        String messageId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        MessageType type = request.getType() != null ? request.getType() : MessageType.TEXT;

        // 발신자는 메시지 전송 시 자동으로 읽음 처리
        Map<Long, Instant> readBy = new HashMap<>();
        readBy.put(request.getSenderUserId(), now);

        // 채팅방 참여자 수 조회 (캐시 우선)
        int totalParticipants = chatRoomCacheService.getParticipantCount(roomId);
        int unreadCount = Math.max(0, totalParticipants - readBy.size());

        // 1. Redis 발행용 응답 객체 생성 (DB 저장 대기 없이 즉시 발행)
        ChatMessageResponse chatMessageResponse = ChatMessageResponse.builder()
                .id(messageId)
                .roomId(roomId)
                .senderUserId(request.getSenderUserId())
                .content(request.getContent())
                .type(type)
                .sentAt(now)
                .readBy(readBy)
                .unreadCount(unreadCount)
                .build();

        // 2. Redis에 메세지 발행 (빠른 응답)
        chatRedisPublisher.publish(chatMessageResponse);

        // 3. Kafka에 저장 이벤트 발행 (비동기 저장)
        ChatMessageEvent event = ChatMessageEvent.builder()
                .id(messageId)
                .roomId(roomId)
                .senderUserId(request.getSenderUserId())
                .content(request.getContent())
                .type(type)
                .sentAt(now)
                .readBy(readBy)
                .build();
        chatKafkaProducer.send(event);
    }
}


