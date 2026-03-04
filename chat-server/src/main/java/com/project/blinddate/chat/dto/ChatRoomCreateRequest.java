package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "채팅방 생성 요청")
public class ChatRoomCreateRequest {

    @Schema(description = "참여 유저 ID 목록", example = "[1, 2]")
    @NotEmpty
    private final List<Long> participantUserIds;
}


