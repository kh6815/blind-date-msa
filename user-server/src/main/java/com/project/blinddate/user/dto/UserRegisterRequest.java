package com.project.blinddate.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "회원가입 요청")
public class UserRegisterRequest {

    @Schema(description = "이메일", example = "user@example.com")
    @Email
    @NotEmpty
    private String email;

    @Schema(description = "비밀번호", example = "password1234!")
    @NotEmpty
    private String password;

    @Schema(description = "닉네임", example = "춘식이")
    @NotEmpty
    private String nickname;

    @Schema(description = "성별", example = "MALE")
    @NotEmpty
    private String gender;

    @Schema(description = "생년월일", example = "1995-05-10")
    @NotNull
    private LocalDate birthDate;

    @Schema(description = "MBTI", example = "INTJ")
    private String mbti;

    @Schema(description = "관심사(콤마 구분 문자열)", example = "축구,영화,여행")
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
}


