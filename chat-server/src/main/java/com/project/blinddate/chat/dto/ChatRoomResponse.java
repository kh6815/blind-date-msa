package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "채팅방 응답 DTO")
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "room-1")
    private final String id;

    @Schema(description = "참여 유저 ID 목록", example = "[1, 2]")
    private final List<Long> participantUserIds;

    @Schema(description = "생성 시각", example = "2025-01-01T10:00:00Z")
    private final Instant createdAt;

    @Schema(description = "마지막 메시지 시각", example = "2025-01-01T10:10:00Z")
    private final Instant lastMessageAt;

    // UI 표시용 추가 필드
    @Schema(description = "상대방 프로필 이미지 URL")
    private final String targetUserImageUrl;

    @Schema(description = "상대방 닉네임")
    private final String targetUserNickname;

    @Schema(description = "상대방 접속 여부")
    private final Boolean isTargetUserOnline;

    @Schema(description = "마지막 메시지 미리보기")
    private final String lastMessagePreview;
}


