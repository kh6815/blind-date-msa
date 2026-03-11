package com.project.blinddate.chat.aop;

import com.project.blinddate.chat.dto.ChatUserIdRequest;
import com.project.blinddate.chat.external.user_client.UserFeignClient;
import com.project.blinddate.common.dto.ResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private ProceedingJoinPoint joinPoint;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("REST 컨트롤러 호출 시 토큰에서 UserId를 조회해 ChatUserIdRequest에 주입한다")
    void aroundRestController_injectsUserId() throws Throwable {
        try {
            ChatUserIdRequest arg = ChatUserIdRequest.builder().currentUserId(null).build();
            Object[] args = new Object[]{arg};

            given(joinPoint.getArgs()).willReturn(args);

            Cookie cookie = new Cookie("Authorization", "Bearer test-token");
            given(request.getCookies()).willReturn(new Cookie[]{cookie});

            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            given(userFeignClient.validateToken(any())).willReturn(ResponseDto.ok(10L));
            given(joinPoint.proceed(any(Object[].class))).willAnswer(invocation -> invocation.getArgument(0));

            Object result = chatUserActivityAspect.aroundRestController(joinPoint);

            Object[] proceededArgs = (Object[]) result;
            ChatUserIdRequest proceeded = (ChatUserIdRequest) proceededArgs[0];
            assertThat(proceeded.getCurrentUserId()).isEqualTo(10L);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}

