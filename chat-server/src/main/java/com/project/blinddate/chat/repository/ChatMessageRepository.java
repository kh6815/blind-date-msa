package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId, Pageable pageable);

    List<ChatMessage> findTop50ByRoomIdOrderBySentAtDesc(String roomId);

    // 전체 메시지 조회용
    List<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId);
}


