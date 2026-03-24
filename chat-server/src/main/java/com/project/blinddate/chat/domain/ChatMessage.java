package com.project.blinddate.chat.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 메시지를 읽은 사용자와 읽은 시간 (userId -> readAt)
     * 발신자는 메시지 전송 시 자동으로 읽음 처리됨
     */
    private Map<Long, Instant> readBy;

    @Builder
    private ChatMessage(String id, String roomId, Long senderUserId, String content, MessageType type, Instant sentAt, Map<Long, Instant> readBy) {
        this.id = id;
        this.roomId = roomId;
        this.senderUserId = senderUserId;
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
        this.sentAt = sentAt;
        this.readBy = readBy != null ? readBy : new HashMap<>();
    }

    /**
     * 특정 사용자가 메시지를 읽음 처리
     * @param userId 읽은 사용자 ID
     * @param readAt 읽은 시간
     */
    public void markAsReadBy(Long userId, Instant readAt) {
        if (this.readBy == null) {
            this.readBy = new HashMap<>();
        }
        this.readBy.put(userId, readAt);
    }

    /**
     * 특정 사용자가 메시지를 읽었는지 확인
     * @param userId 확인할 사용자 ID
     * @return 읽음 여부
     */
    public boolean isReadBy(Long userId) {
        return this.readBy != null && this.readBy.containsKey(userId);
    }
}


