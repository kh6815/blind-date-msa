package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "채팅 메시지 읽음 이벤트 DTO (WebSocket 전송용)")
public class ChatMessageReadEvent {

    @Schema(description = "채팅방 ID", example = "room-1")
    private String roomId;

    @Schema(description = "메시지를 읽은 사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "읽은 시간 (이 시간 이전의 모든 메시지가 읽음 처리됨)", example = "2025-01-01T10:05:00Z")
    private Instant readAt;

    @Schema(description = "읽음 처리된 메시지 개수", example = "5")
    private Integer readMessageCount;
}