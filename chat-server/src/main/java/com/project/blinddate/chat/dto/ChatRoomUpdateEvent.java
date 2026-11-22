package com.project.blinddate.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 사용자별 채팅방 업데이트 이벤트
 * - 채팅방 생성/업데이트/삭제 시 해당 유저의 토픽으로 발행
 * - CREATED: 전체 채팅방 정보 (chatRoom)
 * - UPDATED: 변경된 필드만 (roomId + 변경 필드들)
 * - DELETED: 채팅방 ID만 (roomId)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatRoomUpdateEvent {

    /**
     * 업데이트 타입
     */
    private UpdateType updateType;

    /**
     * 채팅방 ID (UPDATED, DELETED 시 필수)
     */
    private String roomId;

    /**
     * 전체 채팅방 정보 (CREATED 시에만 포함)
     */
    private ChatRoomResponse chatRoom;

    /**
     * 마지막 메시지 미리보기 (UPDATED 시)
     */
    private String lastMessagePreview;

    /**
     * 마지막 메시지 시간 (UPDATED 시)
     */
    private Instant lastMessageAt;

    /**
     * 읽지 않은 메시지 수 증분 (UPDATED 시)
     * 예: +1 (새 메시지), -5 (읽음 처리)
     */
    private Integer unreadCountDelta;

    /**
     * 업데이트 타입
     */
    public enum UpdateType {
        /**
         * 새 채팅방 생성 (chatRoom 포함)
         */
        CREATED,

        /**
         * 채팅방 업데이트 (변경된 필드만)
         */
        UPDATED,

        /**
         * 채팅방 삭제 (roomId만)
         */
        DELETED
    }
}