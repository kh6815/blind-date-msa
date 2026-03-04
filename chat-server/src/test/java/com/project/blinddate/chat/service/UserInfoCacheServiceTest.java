package com.project.blinddate.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.external.user_client.UserFeignClient;
import com.project.blinddate.chat.external.user_client.dto.UserFeignResponse;
import com.project.blinddate.common.dto.ResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserInfoCacheServiceTest {

    @InjectMocks
    private UserInfoCacheService userInfoCacheService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserFeignClient userFeignClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("캐시 히트 시 Redis에서 가져온 데이터를 반환한다")
    void getUserInfo_CacheHit() throws JsonProcessingException {
        // given
        Long userId = 1L;
        String cacheKey = "user:info:" + userId;
        ChatUserInfoResponse cachedInfo = ChatUserInfoResponse.builder()
                .userId(userId)
                .nickname("CachedUser")
                .profileImageUrl("http://cached.image")
                .build();
        String cachedJson = objectMapper.writeValueAsString(cachedInfo);

        given(valueOperations.get(cacheKey)).willReturn(cachedJson);

        // when
        ChatUserInfoResponse result = userInfoCacheService.getUserInfo(userId);

        // then
        assertThat(result.userId).isEqualTo(userId);
        assertThat(result.nickname).isEqualTo("CachedUser");
        assertThat(result.profileImageUrl).isEqualTo("http://cached.image");

        verify(userFeignClient, never()).getUserInfo(anyLong());
    }

    @Test
    @DisplayName("캐시 미스 시 UserFeignClient를 호출하고 결과를 캐시에 저장한다")
    void getUserInfo_CacheMiss_FeignSuccess() throws JsonProcessingException {
        // given
        Long userId = 1L;
        String cacheKey = "user:info:" + userId;
        
        given(valueOperations.get(cacheKey)).willReturn(null);

        // Mock Feign Response
        UserFeignResponse feignResponse = new UserFeignResponse();
        ReflectionTestUtils.setField(feignResponse, "id", userId);
        ReflectionTestUtils.setField(feignResponse, "nickname", "FeignUser");
        ReflectionTestUtils.setField(feignResponse, "profileImageUrl", "http://feign.image");

        ResponseDto<UserFeignResponse> responseDto = ResponseDto.ok(feignResponse);
        given(userFeignClient.getUserInfo(userId)).willReturn(responseDto);

        // when
        ChatUserInfoResponse result = userInfoCacheService.getUserInfo(userId);

        // then
        assertThat(result.userId).isEqualTo(userId);
        assertThat(result.nickname).isEqualTo("FeignUser");
        assertThat(result.profileImageUrl).isEqualTo("http://feign.image");

        verify(userFeignClient, times(1)).getUserInfo(userId);
        verify(valueOperations, times(1)).set(eq(cacheKey), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("Feign 호출 실패 시 기본값을 반환한다")
    void getUserInfo_FeignFailure() {
        // given
        Long userId = 1L;
        String cacheKey = "user:info:" + userId;

        given(valueOperations.get(cacheKey)).willReturn(null);
        given(userFeignClient.getUserInfo(userId)).willThrow(new RuntimeException("Feign Error"));

        // when
        ChatUserInfoResponse result = userInfoCacheService.getUserInfo(userId);

        // then
        assertThat(result.userId).isEqualTo(userId);
        assertThat(result.nickname).isEqualTo("상대방");
        assertThat(result.profileImageUrl).isNull();

        verify(userFeignClient, times(1)).getUserInfo(userId);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }
}
