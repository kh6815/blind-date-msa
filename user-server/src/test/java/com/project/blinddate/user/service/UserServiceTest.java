package com.project.blinddate.user.service;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import com.project.blinddate.user.dto.UserSearchCondition;
import com.project.blinddate.user.dto.UserUpdateRequest;
import com.project.blinddate.user.mapper.UserMapper;
import com.project.blinddate.user.repository.UserImageRepository;
import com.project.blinddate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserImageRepository userImageRepository;

    @Mock
    private UserKafkaProducer userKafkaProducer;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("login - 이메일/비밀번호가 일치하면 UserResponse를 반환한다")
    void login_success() {
        String email = "test@example.com";
        String password = "plain-password";

        User user = User.builder()
                .id(1L)
                .email(email)
                .passwordHash(password)
                .nickname("tester")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        UserResponse mapped = UserResponse.builder()
                .id(1L)
                .email(email)
                .nickname("tester")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userMapper.toResponse(user)).willReturn(mapped);

        UserResponse result = userService.login(email, password);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("login - 잘못된 비밀번호이면 예외를 던진다")
    void login_wrongPassword() {
        String email = "test@example.com";
        String password = "plain-password";

        User user = User.builder()
                .id(1L)
                .email(email)
                .passwordHash("other-password")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(email, password))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("register - 중복 이메일이 아니면 유저를 저장한다")
    void register_success() {
        UserRegisterRequest request = new UserRegisterRequest();
        // 필요 필드만 간단히 설정
        request.setEmail("new@example.com");

        User entity = User.builder()
                .id(1L)
                .email(request.getEmail())
                .build();

        User saved = User.builder()
                .id(1L)
                .email(request.getEmail())
                .build();

        UserResponse mapped = UserResponse.builder()
                .id(1L)
                .email(request.getEmail())
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userMapper.toEntity(request)).willReturn(entity);
        given(userRepository.save(entity)).willReturn(saved);
        given(userMapper.toResponse(saved)).willReturn(mapped);

        UserResponse result = userService.register(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(request.getEmail());
    }

    @Test
    @DisplayName("getUser - 유저와 이미지 정보를 조회하고 온라인 여부를 포함한다")
    void getUser_success() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("user")
                .build();

        UserImage image1 = UserImage.builder()
                .user(user)
                .imageUrl("http://image1")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userImageRepository.findByUser(user)).willReturn(List.of(image1));
        // 온라인 여부 체크용 Redis mock
        given(redisTemplate.hasKey(anyString())).willReturn(true);

        UserResponse result = userService.getUser(userId);

        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getImageUrls()).containsExactly("http://image1");
        assertThat(result.getIsOnline()).isTrue();
    }

    @Test
    @DisplayName("searchUsers(거리순) - 현재 유저 위치 기준으로 거리순 정렬된 결과를 반환한다")
    void searchUsers_withDistanceSort() {
        UserSearchCondition condition = new UserSearchCondition();
        PageRequest pageable = PageRequest.of(0, 10);

        User current = User.builder()
                .id(1L)
                .latitude(37.5)
                .longitude(127.0)
                .build();
        UserResponse currentResponse = UserResponse.builder()
                .id(1L)
                .latitude(37.5)
                .longitude(127.0)
                .build();

        User userNear = User.builder()
                .id(2L)
                .latitude(37.51)
                .longitude(127.01)
                .build();
        User userFar = User.builder()
                .id(3L)
                .latitude(37.7)
                .longitude(127.2)
                .build();

        Page<User> userPage = new PageImpl<>(List.of(userNear, userFar));

        given(userRepository.searchUsers(any(UserSearchCondition.class), any())).willReturn(userPage);
        given(userMapper.toResponse(userNear)).willReturn(UserResponse.builder().id(2L).latitude(userNear.getLatitude()).longitude(userNear.getLongitude()).build());
        given(userMapper.toResponse(userFar)).willReturn(UserResponse.builder().id(3L).latitude(userFar.getLatitude()).longitude(userFar.getLongitude()).build());
        given(redisTemplate.hasKey(anyString())).willReturn(false);

        Page<UserResponse> result = userService.searchUsers(condition, pageable, currentResponse);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("updateProfile - 프로필을 수정하고 Kafka 이벤트를 발행한다")
    void updateProfile_success() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("before")
                .build();

        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("after");
        request.setProfileImageUrl("http://new.image");

        UserResponse baseResponse = UserResponse.builder()
                .id(userId)
                .email(user.getEmail())
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        UserImage image = UserImage.builder()
                .user(user)
                .imageUrl("http://new.image")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toResponse(user)).willReturn(baseResponse);
        given(userImageRepository.findByUser(user)).willReturn(List.of(image));

        UserResponse result = userService.updateProfile(userId, request);

        assertThat(result.getNickname()).isEqualTo("after");
        assertThat(result.getImageUrls()).containsExactly("http://new.image");

        verify(userKafkaProducer).sendUserInfoUpdated(eq(userId), eq("after"), eq("http://new.image"));
    }
}

