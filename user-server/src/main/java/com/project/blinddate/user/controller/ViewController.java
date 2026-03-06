package com.project.blinddate.user.controller;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import com.project.blinddate.user.dto.UserSearchCondition;
import com.project.blinddate.user.dto.UserUpdateRequest;
import com.project.blinddate.user.service.UserService;
import com.project.blinddate.user.service.UserImageStorageService;
import com.project.blinddate.user.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final UserImageStorageService userImageStorageService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;

    @Value("${user.auth.key-prefix}")
    private String USER_TOKEN_KEY_PREFIX;

    private static final Duration USER_ACTIVITY_TTL = Duration.ofMinutes(30);
    private static final Duration USER_TOKEN_TTL = Duration.ofHours(24);

    @Value("${web.cookie.domain}")
    private String COOKIE_DOMAIN;

    @GetMapping("/")
    public String home(
            Model model,
            HttpServletRequest request,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserResponse currentUser = resolveCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }
        UserResponse freshUser = userService.getUser(currentUser.getId());

        PageRequest pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage = userService.searchUsers(condition, pageable, freshUser);

        // Remove current user from list (in page content)
        List<UserResponse> users = new ArrayList<>(userPage.getContent());
        users.removeIf(u -> u.getId().equals(freshUser.getId()));

        model.addAttribute("users", users);
        model.addAttribute("hasNext", userPage.hasNext());
        model.addAttribute("nextPage", userPage.hasNext() ? userPage.getNumber() + 1 : userPage.getNumber());
        model.addAttribute("size", size);
        model.addAttribute("currentUser", freshUser);
        model.addAttribute("activeTab", "home");
        return "home";
    }

    @GetMapping("/users/list")
    public String getUserList(
            Model model,
            HttpServletRequest request,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserResponse currentUser = resolveCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }

        UserResponse freshUser = userService.getUser(currentUser.getId());

        PageRequest pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage = userService.searchUsers(condition, pageable, freshUser);

        // Remove current user from list
        List<UserResponse> users = new ArrayList<>(userPage.getContent());
        users.removeIf(u -> u.getId().equals(freshUser.getId()));

        model.addAttribute("users", users);
        return "home :: userItems";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(String email, String password, Model model, HttpServletRequest httpRequest, HttpServletResponse response) {
        try {
            UserResponse user = userService.login(email, password);

            // JWT Token 생성 및 Redis 저장 (로그인 세션) - Key를 토큰으로 설정 (유효성 검증용)
            String token = jwtTokenProvider.createToken(user.getId());
            String tokenKey = USER_TOKEN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(tokenKey, String.valueOf(user.getId()), USER_TOKEN_TTL);

            // 온라인 상태 저장 (활동 감지용) - Key를 userId로 설정 (조회 편의성)
            String presenceKey = USER_PRESENCE_KEY_PREFIX + user.getId();
            redisTemplate.opsForValue().set(presenceKey, "online", USER_ACTIVITY_TTL);

            // 쿠키에 JWT 저장 (URLEncoder로 공백/특수문자 처리)
            // Bearer prefix는 쿠키 값에 포함하거나, 서버에서 읽을 때 처리. 여기서는 포함.
            String cookieValue = "Bearer " + token;
            cookieValue = URLEncoder.encode(cookieValue, StandardCharsets.UTF_8);

            Cookie cookie = new Cookie("Authorization", cookieValue);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            cookie.setDomain(COOKIE_DOMAIN);
            response.addCookie(cookie);

            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
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
            String tokenKey = USER_TOKEN_KEY_PREFIX + token;
            redisTemplate.delete(tokenKey);
        }
        Cookie cookie = new Cookie("Authorization", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model, HttpServletRequest request) {
        UserResponse currentUser = resolveCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }

        UserResponse targetUser = userService.getUser(id);
        model.addAttribute("user", targetUser);
        model.addAttribute("currentUser", currentUser);
        return "user-detail";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        UserResponse user = resolveCurrentUser(request);
        if (user == null) {
            return "redirect:/login";
        }
        user = userService.getUser(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("activeTab", "profile");
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, HttpServletRequest request) {
        UserResponse user = resolveCurrentUser(request);
        if (user == null) {
            return "redirect:/login";
        }
        user = userService.getUser(user.getId());
        model.addAttribute("user", user);
        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            @ModelAttribute UserUpdateRequest request,
            @RequestParam(value = "profileImageFile", required = false)
            MultipartFile profileImageFile,
            @RequestParam(value = "profileImageFiles", required = false)
            List<MultipartFile> profileImageFiles,
            HttpServletRequest httpRequest
    ) {
        UserResponse user = resolveCurrentUser(httpRequest);
        if (user == null) {
            return "redirect:/login";
        }

        // 삭제된 이미지 처리
        if (request.getDeletedImages() != null && !request.getDeletedImages().isEmpty()) {
            userImageStorageService.deleteUserImages(request.getDeletedImages());
        }

        String imageUrl = userImageStorageService.uploadProfileImage(user.getId(), profileImageFile);
        if (imageUrl != null) {
            request.setProfileImageUrl(imageUrl);
        }
        // 다중 이미지 업로드 처리
        if (profileImageFiles != null && !profileImageFiles.isEmpty()) {
            for (MultipartFile file : profileImageFiles) {
                if (file != null && !file.isEmpty()) {
                    String url = userImageStorageService.uploadProfileImage(user.getId(), file);
                    if (url != null) {
                        // UserImage 엔티티 저장
                        UserImage userImage = UserImage.builder()
                                .user(User.builder().id(user.getId()).build())
                                .imageUrl(url)
                                .build();
                        userImageStorageService.saveUserImage(userImage);
                    }
                }
            }
        }
        UserResponse updatedUser = userService.updateProfile(user.getId(), request);
        return "redirect:/profile";
    }

    private UserResponse resolveCurrentUser(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return null;
        }
        String tokenKey = USER_TOKEN_KEY_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null) {
            return null;
        }
        try {
            Long tokenUserId = jwtTokenProvider.getUserId(token);
            Long redisUserId = Long.valueOf(userIdStr);
            if (!tokenUserId.equals(redisUserId)) {
                return null;
            }
            return userService.getUser(tokenUserId);
        } catch (Exception e) {
            return null;
        }
    }
}
