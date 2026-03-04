package com.project.blinddate.chat.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

// MongoDB용 마이그레이션(= flyway)
@ChangeUnit(id = "init-chat-db", order = "001", author = "user")
public class DatabaseChangelog {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("chat_rooms")) {
            mongoTemplate.createCollection("chat_rooms");
        }
        if (!mongoTemplate.collectionExists("chat_messages")) {
            mongoTemplate.createCollection("chat_messages");
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        // Rollback logic (optional for initial creation)
    }
}
