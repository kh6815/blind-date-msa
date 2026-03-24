package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "채팅방 읽지 않은 메시지 수 응답 DTO")
public class ChatRoomUnreadCountResponse {

    @Schema(description = "채팅방 ID", example = "room-1")
    private String roomId;

    @Schema(description = "읽지 않은 메시지 수", example = "5")
    private Long unreadCount;
}