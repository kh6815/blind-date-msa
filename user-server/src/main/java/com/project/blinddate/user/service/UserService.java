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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // Simple password check (plaintext for now as per init data)
        if (!user.getPasswordHash().equals(password)) {
             throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
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
        java.util.List<String> imageUrls = userImageRepository.findByUser(user)
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

        // Fetch all users matching condition (limit 1000 for safety)
        Pageable largePage = PageRequest.of(0, 1000);
        Page<User> userPage = userRepository.searchUsers(condition, largePage);

        List<UserResponse> sortedUsers = userPage.getContent().stream()
                .map(user -> {
                    UserResponse response = userMapper.toResponse(user);
                    response.setIsOnline(isUserOnline(response.getId()));

                    if (user.getLatitude() != null && user.getLongitude() != null) {
                        double dist = calculateDistance(
                                currentUser.getLatitude(), currentUser.getLongitude(),
                                user.getLatitude(), user.getLongitude()
                        );
                        return UserResponse.builder()
                                .id(response.getId())
                                .email(response.getEmail())
                                .nickname(response.getNickname())
                                .gender(response.getGender())
                                .birthDate(response.getBirthDate())
                                .mbti(response.getMbti())
                                .interests(response.getInterests())
                                .profileImageUrl(response.getProfileImageUrl())
                                .imageUrls(response.getImageUrls())
                                .job(response.getJob())
                                .description(response.getDescription())
                                .location(response.getLocation())
                                .latitude(response.getLatitude())
                                .longitude(response.getLongitude())
                                .distance(dist)
                                .isOnline(isUserOnline(response.getId()))
                                .build();
                    }
                    return response;
                })
                .sorted((u1, u2) -> {
                    if (u1.getDistance() == null && u2.getDistance() == null) return u1.getId().compareTo(u2.getId());
                    if (u1.getDistance() == null) return 1;
                    if (u2.getDistance() == null) return -1;
                    int distCompare = u1.getDistance().compareTo(u2.getDistance());
                    if (distCompare != 0) return distCompare;
                    return u1.getId().compareTo(u2.getId());
                })
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedUsers.size());

        if (start > sortedUsers.size()) {
            return new PageImpl<>(List.of(), pageable, sortedUsers.size());
        }

        return new PageImpl<>(sortedUsers.subList(start, end), pageable, sortedUsers.size());
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
        List<String> imageUrls = userImageRepository.findByUser(user)
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


