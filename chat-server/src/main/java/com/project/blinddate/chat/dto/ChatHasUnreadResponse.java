package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "읽지 않은 메시지 존재 여부 응답")
public class ChatHasUnreadResponse {

    @Schema(description = "읽지 않은 메시지 존재 여부", example = "true")
    private Boolean hasUnread;
}
