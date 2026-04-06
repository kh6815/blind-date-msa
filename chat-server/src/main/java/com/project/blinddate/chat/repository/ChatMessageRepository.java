package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String>, ChatMessageRepositoryCustom {

    Page<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId, Pageable pageable);

    // 최근 메시지부터 조회 (무한 스크롤용)
    Page<ChatMessage> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);

    List<ChatMessage> findTop50ByRoomIdOrderBySentAtDesc(String roomId);

    // 전체 메시지 조회용
    List<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId);

    // 마지막 메시지 단건 조회
    Optional<ChatMessage> findTop1ByRoomIdOrderBySentAtDesc(String roomId);
}


