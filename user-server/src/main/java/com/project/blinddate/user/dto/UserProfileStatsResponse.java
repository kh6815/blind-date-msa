package com.project.blinddate.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "유저 프로필 통계 응답 DTO")
public class UserProfileStatsResponse {

    @Schema(description = "프로필 완성도 (%)", example = "85")
    private Integer completeness;

    @Schema(description = "조회수", example = "127")
    private Long viewCount;

    @Schema(description = "좋아요 수", example = "34")
    private Long likeCount;
}
