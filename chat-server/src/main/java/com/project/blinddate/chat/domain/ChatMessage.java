package com.project.blinddate.chat.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class ChatMessage {

    @Id
    private String id;

    private String roomId;

    private Long senderUserId;

    private String content;

    private MessageType type;

    private Instant sentAt;

    @Builder
    private ChatMessage(String id, String roomId, Long senderUserId, String content, MessageType type, Instant sentAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderUserId = senderUserId;
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
        this.sentAt = sentAt;
    }
}


