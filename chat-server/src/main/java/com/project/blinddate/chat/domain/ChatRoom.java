package com.project.blinddate.chat.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Document(collection = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class ChatRoom {

    @Id
    private String id;

    private List<Long> participantUserIds;

    private Instant createdAt;

    private Instant lastMessageAt;

    @Builder
    private ChatRoom(String id, List<Long> participantUserIds, Instant createdAt, Instant lastMessageAt) {
        this.id = id;
        this.participantUserIds = participantUserIds;
        this.createdAt = createdAt;
        this.lastMessageAt = lastMessageAt;
    }
}


