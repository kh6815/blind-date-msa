package com.project.blinddate.chat.aop;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${chat.kafka.topics.user-activity:user-activity}")
    private String userActivityTopic;

    @Before("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void updateChatUserActivity(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = resolveToken(request);

                if (token != null) {
                    // Kafka로 토큰 전송 (UserServer에서 검증 및 처리)
                    kafkaTemplate.send(userActivityTopic, token);
                    log.debug("Sent user activity event to Kafka. Token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send user activity event: {}", e.getMessage());
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
