package com.project.blinddate.user.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.dto.UserIdRequest;
import com.project.blinddate.user.dto.UserLocationUpdateRequest;
import com.project.blinddate.user.dto.UserProfileStatsResponse;
import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import com.project.blinddate.user.dto.UserUpdateRequest;
import com.project.blinddate.user.service.UserImageStorageService;
import com.project.blinddate.user.service.UserLikeService;
import com.project.blinddate.user.service.UserService;
import com.project.blinddate.user.service.UserViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@Tag(name = "User API", description = "유저 도메인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.USER_API_PREFIX)
public class UserController {

    private final UserService userService;
    private final UserLikeService userLikeService;
    private final UserViewService userViewService;
    private final UserImageStorageService userImageStorageService;

    @Operation(summary = "회원가입", description = "소개팅 서비스 유저를 등록합니다.")
    @PostMapping
    public ResponseEntity<ResponseDto<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        UserResponse response = userService.register(request);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "현재 로그인한 사용자 정보 조회", description = "JWT 토큰으로 현재 로그인한 사용자 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ResponseDto<UserResponse>> getCurrentUser(
            UserIdRequest userIdRequest
    ) {
        Long userId = userIdRequest.getId();
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "유저 단건 조회", description = "유저 상세 정보를 조회합니다.")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ResponseDto<UserResponse>> getUser(
            @PathVariable Long id
    ) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "유저 조회수 기록", description = "특정 유저의 프로필을 조회했음을 기록합니다. 자기 자신의 프로필 조회는 기록하지 않습니다.")
    @PostMapping("/{id:\\d+}/view")
    public ResponseEntity<ResponseDto<Void>> recordView(
            @PathVariable Long id,
            UserIdRequest userIdRequest
    ) {
        // 조회수 기록 (자기 자신의 프로필 조회는 기록하지 않음)
        userViewService.recordView(userIdRequest.getId(), id);
        return ResponseEntity.ok(ResponseDto.ok(null));
    }

    @Operation(summary = "유저 좋아요 수 조회", description = "특정 유저가 받은 좋아요 수를 조회합니다.")
    @GetMapping("/{id:\\d+}/like-count")
    public ResponseEntity<ResponseDto<Long>> getLikeCount(
            @PathVariable Long id
    ) {
        long count = userLikeService.getLikeCount(id);
        return ResponseEntity.ok(ResponseDto.ok(count));
    }

    @Operation(summary = "유저 목록 조회", description = "필터 조건으로 유저 목록을 페이징하여 조회합니다.")
    @GetMapping("/list")
    public ResponseEntity<ResponseDto<List<UserResponse>>> listUsers(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "최소 나이") @RequestParam(required = false) Integer minAge,
            @Parameter(description = "최대 나이") @RequestParam(required = false) Integer maxAge,
            @Parameter(description = "성별") @RequestParam(required = false) String gender,
            @Parameter(description = "MBTI") @RequestParam(required = false) String mbti,
            @Parameter(description = "지역") @RequestParam(required = false) String location,
            UserIdRequest userIdRequest
    ) {
        List<UserResponse> response = userService.listUsers(userIdRequest.getId(), page, size, minAge, maxAge, gender, mbti, location);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "추천 유저 조회", description = "성별/MBTI/관심사 조건으로 소개팅 추천 유저를 조회합니다.")
    @GetMapping("/recommend")
    public ResponseEntity<ResponseDto<List<UserResponse>>> recommendUsers(
            @Parameter(description = "성별") @RequestParam(required = false) String gender,
            @Parameter(description = "MBTI") @RequestParam(required = false) String mbti,
            @Parameter(description = "관심사 CSV (예: 축구,영화)") @RequestParam(required = false) String interests,
            @Parameter(description = "조회 개수", example = "20") @RequestParam(defaultValue = "20") int limit
    ) {
        List<UserResponse> response = userService.recommendUsers(gender, mbti, interests, limit);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "유저 위치 업데이트", description = "유저의 현재 위치 정보를 업데이트합니다.")
    @PatchMapping("/location")
    public ResponseEntity<ResponseDto<Void>> updateLocation(
            @RequestBody UserLocationUpdateRequest locationRequest,
            UserIdRequest userIdRequest
    ) {
        userService.updateLocation(
                userIdRequest.getId(),
                locationRequest.getLatitude(),
                locationRequest.getLongitude()
        );
        return ResponseEntity.ok(ResponseDto.ok(null));
    }

    @Operation(summary = "프로필 수정", description = "유저 프로필 정보를 수정합니다. 파일 업로드를 포함합니다.")
    @PostMapping("/profile/edit")
    public ResponseEntity<ResponseDto<UserResponse>> editProfile(
            @ModelAttribute UserUpdateRequest request,
            @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
            @RequestParam(value = "profileImageFiles", required = false) List<MultipartFile> profileImageFiles,
            UserIdRequest userIdRequest
    ) {
        Long userId = userIdRequest.getId();

        // 삭제할 이미지 처리
        if (request.getDeletedImages() != null && !request.getDeletedImages().isEmpty()) {
            userImageStorageService.deleteUserImages(request.getDeletedImages());
        }

        // 프로필 이미지 업로드
        String imageUrl = userImageStorageService.uploadProfileImage(userId, profileImageFile);
        if (imageUrl != null) {
            request.setProfileImageUrl(imageUrl);
        }

        // 다중 이미지 업로드 처리
        if (profileImageFiles != null && !profileImageFiles.isEmpty()) {
            for (MultipartFile file : profileImageFiles) {
                if (file != null && !file.isEmpty()) {
                    String url = userImageStorageService.uploadProfileImage(userId, file);
                    if (url != null) {
                        userImageStorageService.saveUserImage(UserImage.builder()
                                .user(User.builder().id(userId).build())
                                .imageUrl(url)
                                .build());
                    }
                }
            }
        }

        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "프로필 통계 조회", description = "유저의 프로필 완성도, 조회수, 좋아요 수를 조회합니다.")
    @GetMapping("/profile/stats")
    public ResponseEntity<ResponseDto<UserProfileStatsResponse>> getProfileStats(
            UserIdRequest userIdRequest
    ) {
        Long userId = userIdRequest.getId();

        // 프로필 완성도 계산
        int completeness = userService.calculateProfileCompleteness(userId);

        // 조회수 조회
        long viewCount = userViewService.getViewCount(userId);

        // 좋아요 수 조회
        long likeCount = userLikeService.getLikeCount(userId);

        UserProfileStatsResponse response = UserProfileStatsResponse.builder()
                .completeness(completeness)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .build();

        return ResponseEntity.ok(ResponseDto.ok(response));
    }
}


