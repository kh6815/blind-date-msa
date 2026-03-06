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

    @Value("${external.user-server.url}")
    private String externalUserServerUrl;

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

            Long currentUserId = resolveUserIdViaUserServer(token);

            for (int i = 0; i < args.length; i++) {
                // ChatUserIdRequest 타입이면 교체
                if (args[i] instanceof ChatUserIdRequest) {
                    try {
                        ChatUserIdRequest chatUserIdRequest = ChatUserIdRequest.builder()
                                .currentUserId(currentUserId)
                                .build();

                        args[i] = chatUserIdRequest; // 교체된 데이터로 세팅
                    } catch (Exception e) {
                        log.error("ChatUserIdRequest 타입 데이터 교체 실패", e);
                    }
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
                        .status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(com.project.blinddate.common.dto.ResponseDto.of(401, "unauthorized", null));
            }
            Long currentUserId = resolveUserIdViaUserServer(token);
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ChatUserIdRequest) {
                    try {
                        ChatUserIdRequest chatUserIdRequest = ChatUserIdRequest.builder()
                                .currentUserId(currentUserId)
                                .build();
                        args[i] = chatUserIdRequest;
                    } catch (Exception e) {
                        log.error("ChatUserIdRequest 타입 데이터 교체 실패", e);
                    }
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

    private Long resolveUserIdViaUserServer(String token) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        try {
            ResponseDto<Long> resp = userFeignClient.validateToken(authHeader);
            if (resp != null && resp.getStatus() == 200 && resp.getData() != null) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.error("JWT 토큰 인증 실패", e);
            return null;
        }
        return null;
    }
}
