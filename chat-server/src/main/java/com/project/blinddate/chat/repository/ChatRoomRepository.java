package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    @Query("{ 'participantUserIds': { $all: ?0, $size: ?1 } }")
    Optional<ChatRoom> findByParticipants(List<Long> participantUserIds, int size);

    List<ChatRoom> findByParticipantUserIdsContains(Long userId);
}


