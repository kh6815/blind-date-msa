package com.project.blinddate.user.controller;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.dto.LikeNotificationResponse;
import com.project.blinddate.user.dto.UserIdRequest;
import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import com.project.blinddate.user.dto.UserSearchCondition;
import com.project.blinddate.user.dto.UserUpdateRequest;
import com.project.blinddate.user.security.JwtTokenProvider;
import com.project.blinddate.user.service.UserImageStorageService;
import com.project.blinddate.user.service.UserLikeService;
import com.project.blinddate.user.service.UserService;
import com.project.blinddate.user.service.UserViewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final UserLikeService userLikeService;
    private final UserImageStorageService userImageStorageService;
    private final UserViewService userViewService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;

    @Value("${user.presence.ttl-minutes}")
    private long USER_PRESENCE_TTL_MINUTES;

    @Value("${user.auth.key-prefix}")
    private String USER_TOKEN_KEY_PREFIX;

    @Value("${user.auth.ttl-minutes}")
    private long USER_AUTH_TTL_MINUTES;

    @Value("${web.cookie.domain}")
    private String COOKIE_DOMAIN;

    @GetMapping("/")
    public String home(
            Model model,
            UserIdRequest userIdRequest,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        UserResponse currentUser = userService.getUser(userIdRequest.getId());
        PageRequest pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage = userService.searchUsers(condition, pageable, currentUser);

        List<UserResponse> users = new ArrayList<>(userPage.getContent());
        users.removeIf(u -> u.getId().equals(currentUser.getId()));

        model.addAttribute("users", users);
        model.addAttribute("hasNext", userPage.hasNext());
        model.addAttribute("nextPage", userPage.hasNext() ? userPage.getNumber() + 1 : userPage.getNumber());
        model.addAttribute("size", size);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeTab", "home");
        return "home";
    }

    @GetMapping("/users/list")
    public String getUserList(
            Model model,
            UserIdRequest userIdRequest,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        UserResponse currentUser = userService.getUser(userIdRequest.getId());
        PageRequest pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage = userService.searchUsers(condition, pageable, currentUser);

        // Remove current user from list
        List<UserResponse> users = new ArrayList<>(userPage.getContent());
        users.removeIf(u -> u.getId().equals(currentUser.getId()));

        model.addAttribute("users", users);
        return "home :: userItems";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(String email, String password, HttpServletResponse response) {
        UserResponse user = userService.login(email, password);

        // JWT Token 생성 및 Redis 저장 (로그인 세션) - Key를 토큰으로 설정 (유효성 검증용)
        String token = jwtTokenProvider.createToken(user.getId());
        redisTemplate.opsForValue().set(USER_TOKEN_KEY_PREFIX + token, String.valueOf(user.getId()), Duration.ofMinutes(USER_AUTH_TTL_MINUTES));
        // 온라인 상태 저장 (활동 감지용) - Key를 userId로 설정 (조회 편의성)
        redisTemplate.opsForValue().set(USER_PRESENCE_KEY_PREFIX + user.getId(), "online", Duration.ofMinutes(USER_PRESENCE_TTL_MINUTES));

        // 쿠키에 JWT 저장 (URLEncoder로 공백/특수문자 처리)
        // Bearer prefix는 쿠키 값에 포함하거나, 서버에서 읽을 때 처리. 여기서는 포함.
        String cookieValue = URLEncoder.encode("Bearer " + token, StandardCharsets.UTF_8);
        ResponseCookie cookie = ResponseCookie.from("Authorization", cookieValue)
                .httpOnly(true)
                .secure(true) // HTTPS enabled
                .path("/")
                .maxAge(24 * 60 * 60) // 1 day
                .domain(COOKIE_DOMAIN)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return "redirect:/";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("userRegisterRequest", new UserRegisterRequest());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute UserRegisterRequest request) {
        userService.register(request);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null) {
            redisTemplate.delete(USER_TOKEN_KEY_PREFIX + token);
        }
        ResponseCookie cookie = ResponseCookie.from("Authorization", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/login";
    }

    @GetMapping("/users/{id:\\d+}")
    public String userDetail(
            @PathVariable Long id,
            Model model,
            UserIdRequest userIdRequest
    ) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        // 조회수 기록 (자기 자신의 프로필 조회는 기록하지 않음)
        userViewService.recordView(userIdRequest.getId(), id);

        UserResponse targetUser = userService.getUser(id);
        boolean isLiked = userLikeService.isLiked(userIdRequest.getId(), id);
        long likeCount = userLikeService.getLikeCount(id);

        model.addAttribute("user", targetUser);
        model.addAttribute("currentUser", userService.getUser(userIdRequest.getId()));
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("likeCount", likeCount);
        return "user-detail";
    }

    @GetMapping("/likes")
    public String likeList(
            Model model,
            UserIdRequest userIdRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        PageRequest pageable = PageRequest.of(page, size);
        Page<LikeNotificationResponse> likers = userLikeService.getLikers(userIdRequest.getId(), pageable);

        model.addAttribute("likers", likers.getContent());
        model.addAttribute("hasNext", likers.hasNext());
        model.addAttribute("nextPage", likers.hasNext() ? likers.getNumber() + 1 : likers.getNumber());
        model.addAttribute("size", size);
        model.addAttribute("currentUser", userService.getUser(userIdRequest.getId()));
        return "like-list";
    }

    @GetMapping("/likes/list")
    public String getLikeList(
            Model model,
            UserIdRequest userIdRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        PageRequest pageable = PageRequest.of(page, size);
        Page<LikeNotificationResponse> likers = userLikeService.getLikers(userIdRequest.getId(), pageable);

        model.addAttribute("likers", likers.getContent());
        return "like-list :: likerItems";
    }

    @GetMapping("/profile")
    public String profile(Model model, UserIdRequest userIdRequest) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        Long userId = userIdRequest.getId();
        UserResponse user = userService.getUser(userId);

        // 프로필 완성도 계산
        int completeness = userService.calculateProfileCompleteness(userId);

        // 조회수 조회
        long viewCount = userViewService.getViewCount(userId);

        // 좋아요 수 조회
        long likeCount = userLikeService.getLikeCount(userId);

        model.addAttribute("user", user);
        model.addAttribute("completeness", completeness);
        model.addAttribute("viewCount", viewCount);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("activeTab", "profile");
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, UserIdRequest userIdRequest) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        model.addAttribute("user", userService.getUser(userIdRequest.getId()));
        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            @ModelAttribute UserUpdateRequest request,
            @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
            @RequestParam(value = "profileImageFiles", required = false) List<MultipartFile> profileImageFiles,
            UserIdRequest userIdRequest
    ) {
        if (userIdRequest.getId() == null) return "redirect:/login";

        Long userId = userIdRequest.getId();

        if (request.getDeletedImages() != null && !request.getDeletedImages().isEmpty()) {
            userImageStorageService.deleteUserImages(request.getDeletedImages());
        }

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

        userService.updateProfile(userId, request);
        return "redirect:/profile";
    }
}
