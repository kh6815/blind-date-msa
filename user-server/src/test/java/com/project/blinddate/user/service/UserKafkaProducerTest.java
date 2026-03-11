package com.project.blinddate.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserKafkaProducerTest {

    @InjectMocks
    private UserKafkaProducer userKafkaProducer;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 정보 변경 이벤트를 직렬화하여 Kafka에 전송한다")
    void sendUserInfoUpdated_success() throws Exception {
        Long userId = 1L;
        String nickname = "tester";
        String imageUrl = "http://image";

        // given
        given(objectMapper.writeValueAsString(UserKafkaProducer.UserInfoUpdatedEvent.builder()
                .userId(userId)
                .nickname(nickname)
                .profileImageUrl(imageUrl)
                .build())).willReturn("{\"userId\":1}");

        // when
        userKafkaProducer.sendUserInfoUpdated(userId, nickname, imageUrl);

        // then
        verify(kafkaTemplate, times(1))
                .send(eq("user-info-updated"), eq(String.valueOf(userId)), anyString());
    }
}

