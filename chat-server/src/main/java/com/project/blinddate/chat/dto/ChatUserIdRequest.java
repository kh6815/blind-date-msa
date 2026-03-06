package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "현재 유저 ID 정보")
public class ChatUserIdRequest {

    @Schema(description = "현재 유저 ID", example = "1")
    @NotEmpty
    private Long currentUserId;
}
