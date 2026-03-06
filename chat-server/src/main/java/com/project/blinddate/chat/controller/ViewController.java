package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.dto.ChatRoomCreateRequest;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import com.project.blinddate.chat.dto.ChatUserIdRequest;
import com.project.blinddate.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ChatService chatService;

    @GetMapping("/chats")
    public String chatList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           ChatUserIdRequest chatUserIdRequest,
                           Model model) {
        Long userId = chatUserIdRequest.getCurrentUserId();

        var pageable = PageRequest.of(page, size);
        var roomPage = chatService.getRoomsByUser(userId, pageable);

        model.addAttribute("rooms", roomPage.getContent());
        model.addAttribute("hasNext", roomPage.hasNext());
        model.addAttribute("nextPage", roomPage.hasNext() ? roomPage.getNumber() + 1 : roomPage.getNumber());
        model.addAttribute("userId", userId);
        model.addAttribute("activeTab", "chat");
        return "chat-list";
    }

    @GetMapping("/chats/room/{roomId}")
    public String chatRoom(@PathVariable String roomId,
                           ChatUserIdRequest chatUserIdRequest,
                           Model model) {
        Long userId = chatUserIdRequest.getCurrentUserId();
        ChatRoomResponse room = chatService.getRoom(roomId, userId);

        Long targetUserId = room.getParticipantUserIds().stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);
        model.addAttribute("targetUserId", targetUserId);
        model.addAttribute("targetUserNickname", room.getTargetUserNickname());
        model.addAttribute("targetUserImageUrl", room.getTargetUserImageUrl());
        model.addAttribute("isTargetUserOnline", room.getIsTargetUserOnline());
        return "chat-room";
    }

    @GetMapping("/chats/room/create")
    public String createRoom(@RequestParam Long targetUserId, ChatUserIdRequest chatUserIdRequest) {
        Long myId = chatUserIdRequest.getCurrentUserId();
        ChatRoomCreateRequest dto = ChatRoomCreateRequest.builder()
                .participantUserIds(Arrays.asList(myId, targetUserId))
                .build();
        ChatRoomResponse room = chatService.createRoom(dto);
        return "redirect:/chats/room/" + room.getId();
    }
}
