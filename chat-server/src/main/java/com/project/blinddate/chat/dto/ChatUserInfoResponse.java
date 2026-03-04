package com.project.blinddate.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserInfoResponse {
    public Long userId;
    public String nickname;
    public String profileImageUrl;
}