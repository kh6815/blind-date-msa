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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserImageRepository userImageRepository;
    private final UserKafkaProducer userKafkaProducer;
    private final StringRedisTemplate redisTemplate;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;

    @Transactional
    public UserResponse login(String email, String password) {
        User user = userRepository.findByEmailAndDelYnFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // Simple password check (plaintext for now as per init data)
        if (!user.getPasswordHash().equals(password)) {
             throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmailAndDelYnFalse(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = userMapper.toEntity(request);
        // TODO: 비밀번호 해시 처리 (PasswordEncoder 연동 예정)

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        java.util.List<String> imageUrls = userImageRepository.findByUserAndDelYnFalse(user)
            .stream()
            .map(UserImage::getImageUrl)
            .toList();
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .gender(user.getGender())
            .birthDate(user.getBirthDate())
            .mbti(user.getMbti())
            .interests(user.getInterests())
            .profileImageUrl(user.getProfileImageUrl())
            .job(user.getJob())
            .description(user.getDescription())
            .location(user.getLocation())
            .latitude(user.getLatitude())
            .longitude(user.getLongitude())
            .imageUrls(imageUrls)
            .isOnline(isUserOnline(user.getId()))
            .build();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(UserSearchCondition condition, Pageable pageable) {
        return userRepository.searchUsers(condition, pageable)
                .map(user -> {
                    UserResponse response = userMapper.toResponse(user);
                    response.setIsOnline(isUserOnline(response.getId()));
                    return response;
                });
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(UserSearchCondition condition, Pageable pageable, UserResponse currentUser) {
        if (currentUser == null || currentUser.getLatitude() == null || currentUser.getLongitude() == null) {
            return searchUsers(condition, pageable);
        }

        double lat = currentUser.getLatitude();
        double lon = currentUser.getLongitude();

        return userRepository.searchUsersSortedByDistance(condition, lat, lon, pageable)
                .map(user -> {
                    double dist = (user.getLatitude() != null && user.getLongitude() != null)
                            ? calculateDistance(lat, lon, user.getLatitude(), user.getLongitude())
                            : 0.0;
                    UserResponse base = userMapper.toResponse(user);
                    return UserResponse.builder()
                            .id(base.getId())
                            .email(base.getEmail())
                            .nickname(base.getNickname())
                            .gender(base.getGender())
                            .birthDate(base.getBirthDate())
                            .mbti(base.getMbti())
                            .interests(base.getInterests())
                            .profileImageUrl(base.getProfileImageUrl())
                            .imageUrls(base.getImageUrls())
                            .job(base.getJob())
                            .description(base.getDescription())
                            .location(base.getLocation())
                            .latitude(base.getLatitude())
                            .longitude(base.getLongitude())
                            .distance(dist)
                            .isOnline(isUserOnline(base.getId()))
                            .build();
                });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344; // to km
        return dist;
    }

    private boolean isUserOnline(Long userId) {
        if (userId == null) return false;
        String key = USER_PRESENCE_KEY_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> recommendUsers(String gender, String mbti, String interestsCsv, int limit) {
        List<String> interests = parseInterests(interestsCsv);
        return userRepository.searchRecommendUsers(gender, mbti, interests, limit)
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        
        user.updateProfile(
                request.getNickname(),
                request.getMbti(),
                request.getInterests(),
                request.getJob(),
                request.getDescription(),
                request.getLocation(),
                request.getLatitude(),
                request.getLongitude(),
                request.getProfileImageUrl()
        );
        
        UserResponse baseResponse = userMapper.toResponse(user);

        // Refresh images from DB to ensure deleted images are gone
        List<String> imageUrls = userImageRepository.findByUserAndDelYnFalse(user)
                .stream()
                .map(UserImage::getImageUrl)
                .toList();
        
        UserResponse response = UserResponse.builder()
                .id(baseResponse.getId())
                .email(baseResponse.getEmail())
                .nickname(baseResponse.getNickname())
                .gender(baseResponse.getGender())
                .birthDate(baseResponse.getBirthDate())
                .mbti(baseResponse.getMbti())
                .interests(baseResponse.getInterests())
                .profileImageUrl(baseResponse.getProfileImageUrl())
                .imageUrls(imageUrls)
                .job(baseResponse.getJob())
                .description(baseResponse.getDescription())
                .location(baseResponse.getLocation())
                .latitude(baseResponse.getLatitude())
                .longitude(baseResponse.getLongitude())
                .distance(baseResponse.getDistance())
                .build();
        
        // Publish event to notify chat-server
        userKafkaProducer.sendUserInfoUpdated(
            response.getId(), 
            response.getNickname(), 
            response.getProfileImageUrl()
        );
        
        return response;
    }

    @Transactional
    public void updateLocation(Long userId, Double latitude, Double longitude) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        user.updateLocation(latitude, longitude);
    }

    private List<String> parseInterests(String interestsCsv) {
        if (!StringUtils.hasText(interestsCsv)) {
            return List.of();
        }
        return Arrays.stream(interestsCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}


