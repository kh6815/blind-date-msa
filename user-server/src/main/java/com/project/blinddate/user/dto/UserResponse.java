package com.project.blinddate.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@Schema(description = "유저 기본 응답 DTO")
public class UserResponse {

    @Schema(description = "유저 ID", example = "1")
    private final Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private final String email;

    @Schema(description = "닉네임", example = "춘식이")
    private final String nickname;

    @Schema(description = "성별", example = "MALE")
    private final String gender;

    @Schema(description = "생년월일", example = "1995-05-10")
    private final LocalDate birthDate;

    @Schema(description = "MBTI", example = "INTJ")
    private final String mbti;

    @Schema(description = "관심사(콤마 구분 문자열)", example = "축구,영화,여행")
    private final String interests;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile/1.png")
    private final String profileImageUrl;

    @Schema(description = "유저 이미지 리스트", example = "['url1','url2']")
    private final List<String> imageUrls;

    @Schema(description = "직업", example = "개발자")
    private final String job;

    @Schema(description = "자기소개", example = "안녕하세요")
    private final String description;

    @Schema(description = "위치", example = "서울 강남구")
    private final String location;

    @Schema(description = "위도", example = "37.123456")
    private final Double latitude;

    @Schema(description = "경도", example = "127.123456")
    private final Double longitude;

    @Schema(description = "거리(km)", example = "1.5")
    private final Double distance;

    @Schema(description = "온라인 여부", example = "true")
    private Boolean isOnline;

    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String[] getInterestList() {
        if (interests == null || interests.isEmpty()) {
            return new String[0];
        }
        return interests.split(",");
    }

    public List<String> getImageUrls() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls;
        }
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            return singletonList("https://randomuser.me/api/portraits/lego/1.jpg");
        }
        return Arrays.stream(profileImageUrl.split(","))
            .map(String::trim)
            .filter(url -> !url.isEmpty())
            .collect(Collectors.toList());
    }
}

