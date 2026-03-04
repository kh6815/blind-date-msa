package com.project.blinddate.chat.external.user_client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserFeignResponse {
    
    private Long id;
    
    private String nickname;
    
    @JsonProperty("profile_image_url")
    private String profileImageUrl;
}
