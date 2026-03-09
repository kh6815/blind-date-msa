package com.project.blinddate.user.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import com.project.blinddate.user.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.USER_API_PREFIX)
public class UserAuthController {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${user.auth.key-prefix}")
    private String USER_TOKEN_KEY_PREFIX;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;

    private static final Duration USER_ACTIVITY_TTL = Duration.ofMinutes(30);

    @PostMapping("/token/validate")
    public ResponseEntity<ResponseDto<Long>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request
    ) {
        String token = jwtTokenProvider.resolveToken(authorization, request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(ResponseDto.of(401, "invalid token", null));
        }
        String tokenKey = USER_TOKEN_KEY_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(ResponseDto.of(401, "session expired", null));
        }

        Long tokenUserId = jwtTokenProvider.getUserId(token);
        Long redisUserId = Long.valueOf(userIdStr);
        if (!tokenUserId.equals(redisUserId)) {
            return ResponseEntity.status(401).body(ResponseDto.of(401, "token mismatch", null));
        }

        updateUserActivity(token);
        return ResponseEntity.ok(ResponseDto.ok(tokenUserId));
    }

    public void updateUserActivity(String token) {
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            // userId를 Key로 사용하여 온라인 상태 갱신
            String key = USER_PRESENCE_KEY_PREFIX + userId;

            redisTemplate.opsForValue().set(key, "online", USER_ACTIVITY_TTL);
            log.debug("Consumed user activity event for userId: {}", userId);
        } else {
            log.warn("Invalid token received in user activity event");
        }
    }
}

