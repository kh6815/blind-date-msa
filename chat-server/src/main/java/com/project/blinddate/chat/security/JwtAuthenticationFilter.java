package com.project.blinddate.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Value("${user.auth.key-prefix}")
    private String USER_TOKEN_KEY_PREFIX;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        log.info("JWT Filter - Token resolved: {}", token != null ? "present" : "null");

        if (token != null) {
            Long userId = resolveUserIdViaRedis(token);
            log.info("JWT Filter - UserId from Redis: {}", userId);
            if (userId != null) {
                // SecurityContext에 인증 정보 저장
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("Set Authentication for user: {}", userId);
            } else {
                log.warn("JWT Filter - Failed to resolve userId from token");
            }
        } else {
            log.warn("JWT Filter - No token found in request");
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.info("Authorization header: {}", bearerToken);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        log.info("Cookies count: {}", cookies != null ? cookies.length : 0);

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("Cookie: name={}, value={}", cookie.getName(), cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
                if ("Authorization".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    try {
                        value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                        log.info("Decoded cookie value starts with Bearer: {}", value.startsWith("Bearer "));
                        if (value.startsWith("Bearer ")) {
                            return value.substring(7);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to decode cookie", e);
                    }
                    return value;
                }
            }
        }
        return null;
    }

    private Long resolveUserIdViaRedis(String token) {
        String key = USER_TOKEN_KEY_PREFIX + token;
        log.info("Looking up Redis key: {}", key.substring(0, Math.min(30, key.length())) + "...");
        String userId = redisTemplate.opsForValue().get(key);
        log.info("Redis lookup result: {}", userId);
        return userId != null ? Long.valueOf(userId) : null;
    }
}
