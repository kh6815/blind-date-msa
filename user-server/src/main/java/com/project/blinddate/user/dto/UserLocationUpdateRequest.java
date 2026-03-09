package com.project.blinddate.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 위치 정보 수정 요청 DTO")
public class UserLocationUpdateRequest {

    @Schema(description = "위도", example = "37.123456")
    private Double latitude;

    @Schema(description = "경도", example = "127.123456")
    private Double longitude;
}
