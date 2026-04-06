package com.project.blinddate.chat.service;

import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageReadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;

    @KafkaListener(topics = "chat-message-save", containerFactory = "chatMessageKafkaListenerContainerFactory")
    public void consumeMessage(ChatMessageEvent event) {
        log.info("Consumed message from Kafka: {}", event);

        // 1. 메시지 저장
        chatService.saveMessage(event);

        // 2. 발신자를 제외한 채팅방 참여자 조회
        List<Long> receivers = chatService.getRoomParticipantsExcept(event.getRoomId(), event.getSenderUserId());
        log.info("📤 [Badge SSE] Room {} - sender: {}, receivers: {}", event.getRoomId(), event.getSenderUserId(), receivers);

        // 3. 받는 사람들에게 뱃지 업데이트 이벤트 발행
        // Redis Pub/Sub을 통해 모든 서버 인스턴스에 브로드캐스트
        // 각 서버는 자신에게 SSE 연결된 사용자에게만 이벤트 전송
        receivers.forEach(receiverId -> {
            chatRedisPublisher.publishUnreadBadgeUpdate(receiverId);
            log.debug("✅ [Badge SSE] Published badge update to Redis for userId: {}", receiverId);
        });
    }

    @KafkaListener(topics = "chat-message-read", containerFactory = "chatMessageReadKafkaListenerContainerFactory")
    public void consumeReadEvent(ChatMessageReadEvent event) {
        log.info("Consumed read event from Kafka: {}", event);
        chatService.processReadEvent(event);
    }
}
