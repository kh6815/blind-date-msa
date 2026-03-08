package com.project.blinddate.chat.dto;

import com.project.blinddate.chat.domain.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {
    private String id;
    private String roomId;
    private Long senderUserId;
    private String content;
    private MessageType type;
    private Instant sentAt;
}
