package com.project.blinddate.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.project.blinddate.user.domain.UserLike;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "좋아요 응답 DTO")
public class UserLikeResponse {

    @Schema(description = "좋아요 여부", example = "true")
    private Boolean isLiked;

    @Schema(description = "좋아요 수", example = "10")
    private Long likeCount;

    @Builder
    public UserLikeResponse(Boolean isLiked, Long likeCount) {
        this.isLiked = isLiked;
        this.likeCount = likeCount;
    }
}

