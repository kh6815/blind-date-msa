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

    @Schema(description = "자신을 제외한 다른 참여자 ID 목록 (서버에서 현재 사용자 ID를 자동으로 추가)", example = "[2]")
    @NotEmpty
    private final List<Long> participantUserIds;
}


