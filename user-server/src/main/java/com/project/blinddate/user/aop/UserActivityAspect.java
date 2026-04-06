package com.project.blinddate.user.aop;

import com.project.blinddate.user.dto.UserIdRequest;
import com.project.blinddate.user.security.JwtTokenProvider;
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
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    @Value("${user.presence.ttl-minutes}")
    private long USER_PRESENCE_TTL_MINUTES;

    @Around("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public Object updateUserActivityAndSetUserId(ProceedingJoinPoint joinPoint) throws Throwable{
        Object[] args = joinPoint.getArgs();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = jwtTokenProvider.resolveToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                // userId를 Key로 사용하여 온라인 상태 갱신, JWT 토큰을 값으로 저장
                String key = USER_PRESENCE_KEY_PREFIX + userId;
                redisTemplate.opsForValue().set(key, "online", Duration.ofMinutes(USER_PRESENCE_TTL_MINUTES));
                log.debug("Updated user activity for userId: {}", userId);

                // UserIdRequest 타입이면 현재 userId로 교체
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof UserIdRequest) {
                        UserIdRequest userIdRequest = UserIdRequest.builder()
                                .id(userId)
                                .build();
                        args[i] = userIdRequest;
                    }
                }
            }
        }

        // 컨트롤러 메소드 호출
        return joinPoint.proceed(args);
    }

    // 토큰 해석은 JwtTokenProvider의 공통 구현을 사용합니다.
}
