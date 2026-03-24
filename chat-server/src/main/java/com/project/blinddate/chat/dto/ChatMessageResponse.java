package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.project.blinddate.chat.domain.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "채팅 메시지 응답 DTO(WebSocket/REST 공용)")
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "msg-1")
    private final String id;

    @Schema(description = "채팅방 ID", example = "room-1")
    private final String roomId;

    @Schema(description = "발신 유저 ID", example = "1")
    private final Long senderUserId;

    @Schema(description = "메시지 내용", example = "안녕하세요!")
    private final String content;

    @Schema(description = "메시지 타입", example = "TEXT")
    private final MessageType type;

    @Schema(description = "전송 시각", example = "2025-01-01T10:00:00Z")
    private final Instant sentAt;

    @Schema(description = "메시지를 읽은 사용자 목록 (userId -> 읽은 시간)", example = "{\"1\": \"2025-01-01T10:05:00Z\", \"2\": \"2025-01-01T10:10:00Z\"}")
    private final Map<Long, Instant> readBy;

    @Schema(description = "안 읽은 사람 수 ", example = "1")
    private final Integer unreadCount;
}


