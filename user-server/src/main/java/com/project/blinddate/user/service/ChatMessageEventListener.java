package com.project.blinddate.user.service;

import com.project.blinddate.common.dto.ChatMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatMessageEventListener {

    @KafkaListener(
            topics = "${chat.kafka.topics.message}",
            containerFactory = "chatMessageKafkaListenerContainerFactory"
    )
    public void onChatMessage(@Payload ChatMessageEvent event) {
        log.info("받은 채팅 메시지 이벤트 - roomId={}, senderUserId={}, content={}",
                event.getRoomId(), event.getSenderUserId(), event.getContent());

        // TODO: 유저 마지막 활동 시각 업데이트, 추천 알고리즘에 반영 등
    }
}


