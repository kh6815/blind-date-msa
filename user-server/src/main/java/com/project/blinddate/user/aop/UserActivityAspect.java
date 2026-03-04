package com.project.blinddate.user.aop;

import com.project.blinddate.user.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UserActivityAspect {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;
    private static final Duration USER_ACTIVITY_TTL = Duration.ofMinutes(30);

    @Before("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void updateUserActivity(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = resolveToken(request);

                if (token != null && jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    // userId를 Key로 사용하여 온라인 상태 갱신
                    String key = USER_PRESENCE_KEY_PREFIX + userId;
                    redisTemplate.opsForValue().set(key, "online", USER_ACTIVITY_TTL);
                    log.debug("Updated user activity for userId: {}", userId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update user activity: {}", e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. Header 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Cookie 확인
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("Authorization".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    try {
                        value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                        if (value.startsWith("Bearer ")) {
                            return value.substring(7);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to decode cookie token: {}", e.getMessage());
                    }
                    return value;
                }
            }
        }
        return null;
    }
}
