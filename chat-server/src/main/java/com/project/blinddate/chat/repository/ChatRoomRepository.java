package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    @Query("{ 'participantUserIds': { $all: ?0, $size: ?1 } }")
    Optional<ChatRoom> findByParticipants(List<Long> participantUserIds, int size);

    List<ChatRoom> findByParticipantUserIdsContains(Long userId);

    /**
     * 사용자가 참여한 채팅방 목록 조회 (최신 메시지 순 정렬)
     * Sort는 Pageable에 포함되어 전달됨
     */
    Page<ChatRoom> findByParticipantUserIdsContains(Long userId, Pageable pageable);
}


