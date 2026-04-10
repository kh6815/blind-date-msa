package com.project.blinddate.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "좋아요 요청 DTO")
public class UserLikeRequest {

    @Schema(description = "좋아요 받는 유저 아이디", example = "101")
    private Long targetUserId;
}
