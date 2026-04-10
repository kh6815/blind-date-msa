package com.project.blinddate.chat.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "chat_user_infos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatUserInfo {

    @Id
    private Long userId;

    private String nickname;

    private String profileImageUrl;

    private boolean deleted;

    private Instant updatedAt;

    @Builder
    private ChatUserInfo(Long userId, String nickname, String profileImageUrl, boolean deleted, Instant updatedAt) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.deleted = deleted;
        this.updatedAt = updatedAt;
    }

    public void update(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
//        this.deleted = false;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }
}
