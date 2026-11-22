package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatUnreadBadgeEvent {

    private Long targetUserId;  // 단일 사용자 뱃지 업데이트용 (하위 호환성)
    private List<Long> targetUserIds;  // 다중 사용자 뱃지 업데이트용 (배치 처리)
}
