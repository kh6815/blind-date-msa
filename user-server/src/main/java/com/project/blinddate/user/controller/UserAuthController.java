package com.project.blinddate.user.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import com.project.blinddate.user.dto.*;
import com.project.blinddate.user.security.JwtTokenProvider;
import com.project.blinddate.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Tag(name = "User API", description = "유저 도메인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.USER_API_PREFIX + "/auth")
public class UserAuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${cookie.domain}")
    private String cookieDomain;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;

    @Value("${user.presence.ttl-minutes}")
    private long USER_PRESENCE_TTL_MINUTES;

    @Value("${user.auth.key-prefix}")
    private String USER_TOKEN_KEY_PREFIX;

    @Value("${user.auth.ttl-minutes}")
    private long USER_AUTH_TTL_MINUTES;

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<UserResponse>> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response
    ) {
        UserResponse user = userService.login(request.getEmail(), request.getPassword());

        // JWT Token 생성
        String token = jwtTokenProvider.createToken(user.getId());

        // Redis에 토큰 저장 (로그인 세션) - Key를 토큰으로 설정 (유효성 검증용)
        redisTemplate.opsForValue().set(USER_TOKEN_KEY_PREFIX + token, String.valueOf(user.getId()), Duration.ofMinutes(USER_AUTH_TTL_MINUTES));
        // 온라인 상태 저장 (활동 감지용) - Key를 userId로 설정 (조회 편의성)
        redisTemplate.opsForValue().set(USER_PRESENCE_KEY_PREFIX + user.getId(), "online", Duration.ofMinutes(USER_PRESENCE_TTL_MINUTES));

        // 쿠키에 JWT 저장
        Cookie cookie = new Cookie("Authorization", URLEncoder.encode("Bearer " + token, StandardCharsets.UTF_8));
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(60 * 60 * 24); // 1일
        response.addCookie(cookie);

        return ResponseEntity.ok(ResponseDto.ok(user));
    }

    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자를 로그아웃합니다. Redis에서 토큰을 삭제하고 쿠키를 제거합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Redis에서 토큰 삭제
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null) {
            redisTemplate.delete(USER_TOKEN_KEY_PREFIX + token);
        }

        // 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("Authorization", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ResponseDto.ok(null));
    }
}


