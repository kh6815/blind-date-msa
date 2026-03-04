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
@Schema(description = "유저 프로필 수정 요청 DTO")
public class UserUpdateRequest {

    @Schema(description = "닉네임", example = "춘식이")
    private String nickname;

    @Schema(description = "MBTI", example = "INTJ")
    private String mbti;

    @Schema(description = "관심사(콤마 구분)", example = "축구,영화,여행")
    private String interests;

    @Schema(description = "직업", example = "개발자")
    private String job;

    @Schema(description = "자기소개", example = "안녕하세요")
    private String description;

    @Schema(description = "위치", example = "서울 강남구")
    private String location;

    @Schema(description = "위도", example = "37.123456")
    private Double latitude;

    @Schema(description = "경도", example = "127.123456")
    private Double longitude;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "삭제할 추가 이미지 URL 리스트")
    private java.util.List<String> deletedImages;
}
