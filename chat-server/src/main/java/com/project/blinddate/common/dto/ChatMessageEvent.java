package com.project.blinddate.common.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 채팅 메시지 Kafka 이벤트 DTO (Chat 서버용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessageEvent {

    private String messageId;
    private String roomId;
    private Long senderUserId;
    private String content;
    private String type;
    private Instant sentAt;
}


