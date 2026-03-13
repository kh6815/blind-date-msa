package com.project.blinddate.chat.aop;

import com.project.blinddate.chat.dto.ChatUserIdRequest;
import com.project.blinddate.chat.external.user_client.UserFeignClient;
import com.project.blinddate.common.dto.ResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ChatUserActivityAspect {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${user.auth.key-prefix}")
    private String USER_TOKEN_KEY_PREFIX;

    @Value("${external.user-server.url}")
    private String externalUserServerUrl;

    private final StringRedisTemplate redisTemplate;
    private final UserFeignClient userFeignClient;

    @Around("@within(org.springframework.stereotype.Controller)")
    public Object aroundViewController(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = resolveToken(request);

            if (token == null) {
                return "redirect:" + externalUserServerUrl + "/login";
            }

            Long currentUserId = resolveUserIdViaRedis(token);
            if (currentUserId == null) {
                return "redirect:" + externalUserServerUrl + "/login";
            }

            for (int i = 0; i < args.length; i++) {
                // ChatUserIdRequest 타입이면 교체
                if (args[i] instanceof ChatUserIdRequest) {
                    ChatUserIdRequest chatUserIdRequest = ChatUserIdRequest.builder()
                            .currentUserId(currentUserId)
                            .build();

                    args[i] = chatUserIdRequest; // 교체된 데이터로 세팅
                }
            }
        }

        // 컨트롤러 메소드 호출
        return joinPoint.proceed(args);
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object aroundRestController(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = resolveToken(request);
            if (token == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseDto.of(401, "unauthorized", null));
            }
            Long currentUserId = resolveUserIdViaRedis(token);
            if (currentUserId == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseDto.of(401, "unauthorized", null));
            }
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ChatUserIdRequest) {
                    ChatUserIdRequest chatUserIdRequest = ChatUserIdRequest.builder()
                            .currentUserId(currentUserId)
                            .build();
                    args[i] = chatUserIdRequest;
                }
            }
        }
        return joinPoint.proceed(args);
    }

    public static String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("Authorization".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    try {
                        value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                        if (value.startsWith("Bearer ")) {
                            return value.substring(7);
                        }
                    } catch (Exception ignored) {}
                    return value;
                }
            }
        }
        return null;
    }

    private Long resolveUserIdViaRedis(String token) {
        String userId = redisTemplate.opsForValue().get(USER_TOKEN_KEY_PREFIX + token);
        return userId != null ? Long.valueOf(userId) : null;
    }
}
