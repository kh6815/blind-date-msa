package com.project.blinddate.chat.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.HashMap;

/**
 * ChatMessage에 readBy 필드 추가 마이그레이션
 * 기존 메시지에 빈 readBy Map 추가
 */
@Slf4j
@ChangeUnit(id = "add-readby-field-to-messages", order = "002", author = "user")
public class AddReadByFieldChangelog {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        log.info("Starting migration: Adding readBy field to existing chat messages");

        // readBy 필드가 없는 모든 메시지 조회
        Query query = new Query(Criteria.where("readBy").exists(false));

        // readBy 필드를 빈 Map으로 설정
        Update update = new Update().set("readBy", new HashMap<>());

        // 모든 해당 메시지 업데이트
        long modifiedCount = mongoTemplate.updateMulti(query, update, "chat_messages").getModifiedCount();

        log.info("Migration completed: Added readBy field to {} chat messages", modifiedCount);
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        log.info("Starting rollback: Removing readBy field from chat messages");

        // readBy 필드가 있는 모든 메시지에서 제거
        Query query = new Query(Criteria.where("readBy").exists(true));
        Update update = new Update().unset("readBy");

        long modifiedCount = mongoTemplate.updateMulti(query, update, "chat_messages").getModifiedCount();

        log.info("Rollback completed: Removed readBy field from {} chat messages", modifiedCount);
    }
}