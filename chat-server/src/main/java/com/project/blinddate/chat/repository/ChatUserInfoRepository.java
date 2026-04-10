package com.project.blinddate.chat.repository;

import com.project.blinddate.chat.domain.ChatUserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatUserInfoRepository extends MongoRepository<ChatUserInfo, Long> {
}
