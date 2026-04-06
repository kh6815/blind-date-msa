package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatMessage;

import java.util.List;

/**
 * ChatMessage 커스텀 Repository 인터페이스
 */
public interface ChatMessageRepositoryCustom {

    /**
     * 특정 사용자가 읽지 않은 최근 메시지를 조회합니다.
     * 성능 최적화: MongoDB 쿼리 레벨에서 필터링하여 최대 limit개만 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param limit  최대 조회 개수
     * @return 읽지 않은 메시지 목록 (최근 메시지부터 내림차순)
     */
    List<ChatMessage> findUnreadMessagesByUserId(String roomId, Long userId, int limit);
}