package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ChatMessage 커스텀 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ChatMessage> findUnreadMessagesByUserId(String roomId, Long userId, int limit) {
        // MongoDB 쿼리: readBy 맵에 userId 키가 존재하지 않는 메시지
        // { "roomId": roomId, "readBy.{userId}": { $exists: false } }
        Query query = new Query();
        query.addCriteria(Criteria.where("roomId").is(roomId));
        query.addCriteria(Criteria.where("readBy." + userId).exists(false));

        // 최근 메시지부터 내림차순 정렬
        query.with(Sort.by(Sort.Direction.DESC, "sentAt"));

        // 최대 limit개만 조회
        query.limit(limit);

        return mongoTemplate.find(query, ChatMessage.class);
    }
}