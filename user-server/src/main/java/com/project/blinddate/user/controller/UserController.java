package com.project.blinddate.user.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import com.project.blinddate.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User API", description = "유저 도메인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.USER_API_PREFIX)
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "소개팅 서비스 유저를 등록합니다.")
    @PostMapping
    public ResponseEntity<ResponseDto<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        UserResponse response = userService.register(request);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "유저 단건 조회", description = "유저 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<UserResponse>> getUser(
            @PathVariable Long id
    ) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "추천 유저 조회", description = "성별/MBTI/관심사 조건으로 소개팅 추천 유저를 조회합니다.")
    @GetMapping("/recommend")
    public ResponseEntity<ResponseDto<List<UserResponse>>> recommendUsers(
            @Parameter(description = "성별") @RequestParam(required = false) String gender,
            @Parameter(description = "MBTI") @RequestParam(required = false) String mbti,
            @Parameter(description = "관심사 CSV (예: 축구,영화)") @RequestParam(required = false) String interests,
            @Parameter(description = "조회 개수", example = "20") @RequestParam(defaultValue = "20") int limit
    ) {
        List<UserResponse> response = userService.recommendUsers(gender, mbti, interests, limit);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }
}


