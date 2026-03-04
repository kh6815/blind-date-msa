package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.dto.ChatMessageSendRequest;
import com.project.blinddate.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chats/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageSendRequest request
    ) {
        chatService.sendMessage(roomId, request.getSenderUserId(), request.getContent(), request.getType());
        // Direct send removed to avoid duplicates with Kafka Consumer
    }
}


