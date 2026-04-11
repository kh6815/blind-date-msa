package com.project.blinddate.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LikeNotificationResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime likedAt;
    private boolean isRead;
}
