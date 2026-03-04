package com.project.blinddate.user.service;

import com.project.blinddate.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActivityConsumer {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;
    
    private static final Duration USER_ACTIVITY_TTL = Duration.ofMinutes(30);

    @KafkaListener(topics = "${user.kafka.topics.activity:user-activity}", groupId = "${spring.kafka.consumer.group-id:user-group}")
    public void consumeUserActivity(String token) {
        try {
            // Remove quotes if present (sometimes Kafka sends strings with quotes depending on serializer)
            // But usually StringSerializer sends raw string.
            // If it's JSON string, we might need to handle it.
            // Assuming ChatServer sends raw string via KafkaTemplate<String, String>.
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                // userId를 Key로 사용하여 온라인 상태 갱신
                String key = USER_PRESENCE_KEY_PREFIX + userId;
                
                redisTemplate.opsForValue().set(key, "online", USER_ACTIVITY_TTL);
                log.debug("Consumed user activity event for userId: {}", userId);
            } else {
                log.warn("Invalid token received in user activity event");
            }
        } catch (Exception e) {
            log.error("Failed to process user activity event: {}", e.getMessage());
        }
    }
}
