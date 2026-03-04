package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomCreateRequest;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import com.project.blinddate.chat.service.ChatService;
import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Chat API", description = "채팅 도메인 REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.CHAT_API_PREFIX)
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 생성", description = "참여 유저 목록으로 새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<ResponseDto<ChatRoomResponse>> createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request
    ) {
        ChatRoomResponse response = chatService.createRoom(request);
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
}


