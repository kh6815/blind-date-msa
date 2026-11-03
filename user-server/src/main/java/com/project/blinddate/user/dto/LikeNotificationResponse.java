package com.project.blinddate.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "좋아요 알림 응답")
public class LikeNotificationResponse {

    @Schema(description = "좋아요를 누른 사용자 ID")
    private Long userId;

    @Schema(description = "좋아요를 누른 사용자 닉네임")
    private String nickname;

    @Schema(description = "좋아요를 누른 사용자 프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "좋아요를 누른 시간")
    private LocalDateTime likedAt;

    @Schema(description = "읽음 여부")
    private boolean isRead;
}
