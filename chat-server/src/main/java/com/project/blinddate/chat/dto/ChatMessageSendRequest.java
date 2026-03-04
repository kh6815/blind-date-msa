package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.project.blinddate.chat.domain.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "채팅 메시지 전송 요청(WebSocket/REST 공용)")
public class ChatMessageSendRequest {

    @Schema(description = "발신 유저 ID", example = "1")
    @NotNull
    private final Long senderUserId;

    @Schema(description = "메시지 내용", example = "안녕하세요!")
    @NotBlank
    private final String content;

    @Schema(description = "메시지 타입", example = "TEXT")
    private final MessageType type;
}


