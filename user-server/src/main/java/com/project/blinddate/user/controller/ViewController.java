package com.project.blinddate.user.controller;

import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import com.project.blinddate.user.dto.UserSearchCondition;
import com.project.blinddate.user.dto.UserUpdateRequest;
import com.project.blinddate.user.service.UserService;
import com.project.blinddate.user.service.UserImageStorageService;
import com.project.blinddate.user.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final UserImageStorageService userImageStorageService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${user.presence.key-prefix}")
    private String USER_PRESENCE_KEY_PREFIX;
    
    @Value("${user.auth.key-prefix:user:token:}")
    private String USER_TOKEN_KEY_PREFIX;
    
    private static final Duration USER_ACTIVITY_TTL = Duration.ofMinutes(30);
    private static final Duration USER_TOKEN_TTL = Duration.ofHours(24);

    @GetMapping("/")
    public String home(
            Model model,
            HttpSession session,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Always refresh current user data to get latest location/info
        UserResponse freshUser = userService.getUser(currentUser.getId());
        session.setAttribute("user", freshUser);

        PageRequest pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage = userService.searchUsers(condition, pageable, freshUser);

        // Remove current user from list (in page content)
        java.util.List<UserResponse> users = new java.util.ArrayList<>(userPage.getContent());
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
            HttpSession session,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Always refresh current user data
        UserResponse freshUser = userService.getUser(currentUser.getId());
        session.setAttribute("user", freshUser);

        PageRequest pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage = userService.searchUsers(condition, pageable, freshUser);

        // Remove current user from list
        java.util.List<UserResponse> users = new java.util.ArrayList<>(userPage.getContent());
        users.removeIf(u -> u.getId().equals(freshUser.getId()));

        model.addAttribute("users", users);
        return "home :: userItems";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(String email, String password, HttpSession session, Model model, HttpServletResponse response) {
        try {
            UserResponse user = userService.login(email, password);
            session.setAttribute("user", user);

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
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        UserResponse targetUser = userService.getUser(id);
        model.addAttribute("user", targetUser);
        model.addAttribute("currentUser", currentUser);
        return "user-detail";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        // Refresh user data
        user = userService.getUser(user.getId());
        session.setAttribute("user", user);
        
        model.addAttribute("user", user);
        model.addAttribute("activeTab", "profile");
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        // Refresh
        user = userService.getUser(user.getId());
        model.addAttribute("user", user);
        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            @ModelAttribute UserUpdateRequest request,
            @org.springframework.web.bind.annotation.RequestParam(value = "profileImageFile", required = false)
            org.springframework.web.multipart.MultipartFile profileImageFile,
            @org.springframework.web.bind.annotation.RequestParam(value = "profileImageFiles", required = false)
            java.util.List<org.springframework.web.multipart.MultipartFile> profileImageFiles,
            HttpSession session
    ) {
        UserResponse user = (UserResponse) session.getAttribute("user");
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
            for (org.springframework.web.multipart.MultipartFile file : profileImageFiles) {
                if (file != null && !file.isEmpty()) {
                    String url = userImageStorageService.uploadProfileImage(user.getId(), file);
                    if (url != null) {
                        // UserImage 엔티티 저장
                        com.project.blinddate.user.domain.UserImage userImage = com.project.blinddate.user.domain.UserImage.builder()
                            .user(com.project.blinddate.user.domain.User.builder().id(user.getId()).build())
                            .imageUrl(url)
                            .build();
                        userImageStorageService.saveUserImage(userImage);
                    }
                }
            }
        }
        UserResponse updatedUser = userService.updateProfile(user.getId(), request);
        session.setAttribute("user", updatedUser);
        return "redirect:/profile";
    }
}
