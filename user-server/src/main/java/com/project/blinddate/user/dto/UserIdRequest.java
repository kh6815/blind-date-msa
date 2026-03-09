package com.project.blinddate.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 위치 정보 수정 요청 DTO")
public class UserIdRequest {

    @Schema(description = "유저 ID", example = "1")
    private Long id;
}
