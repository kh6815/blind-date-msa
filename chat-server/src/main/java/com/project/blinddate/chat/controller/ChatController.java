package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.dto.*;
import com.project.blinddate.chat.service.ChatKafkaProducer;
import com.project.blinddate.chat.service.ChatRedisPublisher;
import com.project.blinddate.chat.service.ChatService;
import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Chat API", description = "채팅 도메인 REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.CHAT_API_PREFIX)
public class ChatController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final ChatKafkaProducer chatKafkaProducer;

    @Operation(summary = "채팅방 목록 조회", description = "해당 유저가 참여하고 있는 채팅방 목록을 페이지 단위로 조회합니다.")
    @GetMapping("/rooms")
    public ResponseEntity<ResponseDto<List<ChatRoomResponse>>> getRooms(
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            ChatUserIdRequest chatUserIdRequest
    ) {
        Long userId = chatUserIdRequest.getCurrentUserId();

        var pageable = PageRequest.of(page, size);
        var roomPage = chatService.getRoomsByUser(userId, pageable);

        List<ChatRoomResponse> response = roomPage.getContent();
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "채팅방 생성", description = "다른 참여자 ID 목록으로 새로운 채팅방을 생성합니다. 현재 사용자는 자동으로 참여자에 포함됩니다.")
    @PostMapping("/rooms")
    public ResponseEntity<ResponseDto<ChatRoomResponse>> createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            ChatUserIdRequest chatUserIdRequest
    ) {
        Long currentUserId = chatUserIdRequest.getCurrentUserId();

        // 현재 사용자 ID를 참여자 목록에 추가
        List<Long> allParticipants = new ArrayList<>(request.getParticipantUserIds());
        allParticipants.add(currentUserId);

        ChatRoomResponse response = chatService.createRoom(
                ChatRoomCreateRequest.builder()
                        .participantUserIds(allParticipants)
                        .build()
        );
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "채팅방 단건 조회", description = "특정 채팅방의 상세 정보를 조회합니다.")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ResponseDto<ChatRoomResponse>> getRoom(
            @PathVariable String roomId,
            ChatUserIdRequest chatUserIdRequest
    ) {
        Long userId = chatUserIdRequest.getCurrentUserId();
        ChatRoomResponse response = chatService.getRoom(roomId, userId);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 메시지 목록을 페이지 단위로 조회합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ResponseDto<List<ChatMessageResponse>>> getMessages(
            @PathVariable String roomId,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "50")
            @RequestParam(defaultValue = "50") int size
    ) {
        List<ChatMessageResponse> response = chatService.getRecentMessages(roomId, page, size);
        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "채팅방 메시지 읽음 처리 (Timestamp 기반)", description = "특정 시간 이전의 모든 메시지를 읽음 처리합니다.")
    @PutMapping("/rooms/{roomId}/messages/read")
    public ResponseEntity<ResponseDto<ChatMessageReadEvent>> markMessagesAsRead(
            @PathVariable String roomId,
            ChatUserIdRequest chatUserIdRequest
    ) {
        Long userId = chatUserIdRequest.getCurrentUserId();
        Instant now = Instant.now();

        // 1. Timestamp 기반 읽음 처리 (readAt 이전의 모든 메시지 읽음)
        int readMessageCount = chatService.markMessagesAsReadByTimestamp(roomId, userId, now);

        // 2. 경량 읽음 이벤트 생성 (메시지 ID 목록 대신 timestamp + 개수만 전송)
        ChatMessageReadEvent event = ChatMessageReadEvent.builder()
                .roomId(roomId)
                .userId(userId)
                .readAt(now)
                .readMessageCount(readMessageCount)
                .build();

        // 3. WebSocket을 통해 읽음 이벤트를 채팅방의 다른 참여자에게 즉시 전송
        chatRedisPublisher.publishReadEvent(event);

        // 4. Kafka에 읽음 이벤트 발행 (백업/감사 로그)
        chatKafkaProducer.sendReadEvent(event);

        return ResponseEntity.ok(ResponseDto.ok(event));
    }

    //TODO 필요한 API인가?
    @Operation(summary = "채팅방 읽지 않은 메시지 수 조회", description = "현재 사용자의 특정 채팅방 내 읽지 않은 메시지 수를 조회합니다.")
    @GetMapping("/rooms/{roomId}/unread-count")
    public ResponseEntity<ResponseDto<ChatRoomUnreadCountResponse>> getUnreadCount(
            @PathVariable String roomId,
            ChatUserIdRequest chatUserIdRequest
    ) {
        Long userId = chatUserIdRequest.getCurrentUserId();
        Long unreadCount = chatService.getUnreadCount(roomId, userId);

        ChatRoomUnreadCountResponse response = ChatRoomUnreadCountResponse.builder()
                .roomId(roomId)
                .unreadCount(unreadCount)
                .build();

        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    @Operation(summary = "읽지 않은 메시지 존재 여부 조회", description = "사용자에게 읽지 않은 메시지가 하나라도 있는지 확인합니다.")
    @GetMapping("/has-unread")
    public ResponseEntity<ResponseDto<ChatHasUnreadResponse>> hasUnreadMessages(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            ChatUserIdRequest chatUserIdRequest
    ) {
        boolean hasUnread = chatService.hasUnreadMessages(chatUserIdRequest.getCurrentUserId());

        ChatHasUnreadResponse response = ChatHasUnreadResponse.builder()
                .hasUnread(hasUnread)
                .build();

        return ResponseEntity.ok(ResponseDto.ok(response));
    }
}


