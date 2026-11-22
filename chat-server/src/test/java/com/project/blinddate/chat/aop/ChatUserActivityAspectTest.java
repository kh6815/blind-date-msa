package com.project.blinddate.chat.aop;

import com.project.blinddate.chat.dto.ChatUserIdRequest;
import com.project.blinddate.chat.external.user_client.UserFeignClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatUserActivityAspectTest {

    @InjectMocks
    private ChatUserActivityAspect chatUserActivityAspect;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("REST 컨트롤러 호출 시 토큰에서 UserId를 조회해 ChatUserIdRequest에 주입한다")
    void aroundRestController_injectsUserId() throws Throwable {
        try {
            // Given
            ReflectionTestUtils.setField(chatUserActivityAspect, "USER_TOKEN_KEY_PREFIX", "user:token:");

            ChatUserIdRequest arg = ChatUserIdRequest.builder().currentUserId(null).build();
            Object[] args = new Object[]{arg};

            given(joinPoint.getArgs()).willReturn(args);

            Cookie cookie = new Cookie("Authorization", "Bearer test-token");
            given(request.getCookies()).willReturn(new Cookie[]{cookie});

            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            // Redis에서 userId 조회 Mock
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn("10");

            given(joinPoint.proceed(any(Object[].class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            Object result = chatUserActivityAspect.aroundRestController(joinPoint);

            // Then
            Object[] proceededArgs = (Object[]) result;
            ChatUserIdRequest proceeded = (ChatUserIdRequest) proceededArgs[0];
            assertThat(proceeded.getCurrentUserId()).isEqualTo(10L);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}

