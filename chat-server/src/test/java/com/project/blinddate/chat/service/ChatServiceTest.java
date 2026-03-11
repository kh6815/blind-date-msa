package com.project.blinddate.chat.service;

import com.project.blinddate.chat.domain.ChatMessage;
import com.project.blinddate.chat.domain.ChatRoom;
import com.project.blinddate.chat.domain.MessageType;
import com.project.blinddate.chat.dto.ChatMessageEvent;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomCreateRequest;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import com.project.blinddate.chat.dto.ChatUserInfoResponse;
import com.project.blinddate.chat.mapper.ChatMapper;
import com.project.blinddate.chat.repository.ChatMessageRepository;
import com.project.blinddate.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private UserInfoCacheService userInfoCacheService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("saveMessage - 채팅 메시지를 저장하고 채팅방의 마지막 메시지 시간을 갱신한다")
    void saveMessage_success() {
        ChatRoom room = ChatRoom.builder()
                .id("room1")
                .participantUserIds(List.of(1L, 2L))
                .createdAt(Instant.now())
                .lastMessageAt(null)
                .build();

        ChatMessageEvent event = ChatMessageEvent.builder()
                .id("msg1")
                .roomId("room1")
                .senderUserId(1L)
                .content("hello")
                .type(MessageType.TEXT)
                .sentAt(Instant.now())
                .build();

        given(chatRoomRepository.findById("room1")).willReturn(Optional.of(room));

        chatService.saveMessage(event);

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("createRoom - 기존 방이 없으면 새로 생성하고 대상 유저 정보를 채운다")
    void createRoom_newRoom() {
        List<Long> participants = List.of(1L, 2L);
        ChatRoomCreateRequest request = ChatRoomCreateRequest.builder()
                .participantUserIds(participants)
                .build();

        ChatRoom savedRoom = ChatRoom.builder()
                .id("room1")
                .participantUserIds(participants)
                .createdAt(Instant.now())
                .build();

        given(chatRoomRepository.findByParticipants(participants, participants.size())).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);

        ChatUserInfoResponse userInfo = ChatUserInfoResponse.builder()
                .userId(2L)
                .nickname("상대방닉")
                .profileImageUrl("http://image")
                .build();

        given(userInfoCacheService.getUserInfo(2L)).willReturn(userInfo);
        given(redisTemplate.hasKey(anyString())).willReturn(true);

        ChatRoomResponse response = chatService.createRoom(request);

        assertThat(response.getId()).isEqualTo("room1");
        assertThat(response.getTargetUserNickname()).isEqualTo("상대방닉");
        assertThat(response.getTargetUserImageUrl()).isEqualTo("http://image");
        assertThat(response.getIsTargetUserOnline()).isTrue();
    }

    @Test
    @DisplayName("sendMessage - 메시지를 저장하고 마지막 메시지 시간을 갱신한다")
    void sendMessage_success() {
        ChatRoom room = ChatRoom.builder()
                .id("room1")
                .participantUserIds(List.of(1L, 2L))
                .createdAt(Instant.now())
                .lastMessageAt(null)
                .build();

        given(chatRoomRepository.findById("room1")).willReturn(Optional.of(room));

        ChatMessage saved = ChatMessage.builder()
                .id("msg1")
                .roomId("room1")
                .senderUserId(1L)
                .content("hello")
                .type(MessageType.TEXT)
                .sentAt(Instant.now())
                .build();

        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(saved);

        ChatMessageResponse mapped = ChatMessageResponse.builder()
                .id("msg1")
                .roomId("room1")
                .senderUserId(1L)
                .content("hello")
                .type(MessageType.TEXT)
                .build();

        given(chatMapper.toMessageResponse(saved)).willReturn(mapped);

        ChatMessageResponse result = chatService.sendMessage("room1", 1L, "hello", MessageType.TEXT);

        assertThat(result.getId()).isEqualTo("msg1");
        assertThat(result.getContent()).isEqualTo("hello");

        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("getRecentMessages - 채팅방의 최근 메시지들을 조회한다")
    void getRecentMessages_success() {
        ChatMessage m1 = ChatMessage.builder().id("m1").roomId("room1").build();
        ChatMessage m2 = ChatMessage.builder().id("m2").roomId("room1").build();

        Page<ChatMessage> page = new PageImpl<>(List.of(m1, m2));

        given(chatMessageRepository.findByRoomIdOrderBySentAtAsc(eq("room1"), any(PageRequest.class)))
                .willReturn(page);

        given(chatMapper.toMessageResponse(m1)).willReturn(ChatMessageResponse.builder().id("m1").build());
        given(chatMapper.toMessageResponse(m2)).willReturn(ChatMessageResponse.builder().id("m2").build());

        var result = chatService.getRecentMessages("room1", 0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("m1");
        assertThat(result.get(1).getId()).isEqualTo("m2");
    }

    @Test
    @DisplayName("getRoomsByUser - 유저가 참여한 채팅방 목록을 조회하고 대상 유저 정보를 포함한다")
    void getRoomsByUser_success() {
        Long userId = 1L;
        ChatRoom room = ChatRoom.builder()
                .id("room1")
                .participantUserIds(List.of(1L, 2L))
                .createdAt(Instant.now())
                .build();

        given(chatRoomRepository.findByParticipantUserIdsContains(userId)).willReturn(List.of(room));

        ChatUserInfoResponse userInfo = ChatUserInfoResponse.builder()
                .userId(2L)
                .nickname("상대방닉")
                .profileImageUrl("http://image")
                .build();
        given(userInfoCacheService.getUserInfo(2L)).willReturn(userInfo);

        var pageable = PageRequest.of(0, 10);

        var result = chatService.getRoomsByUser(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        ChatRoomResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo("room1");
        assertThat(response.getTargetUserNickname()).isEqualTo("상대방닉");
        assertThat(response.getTargetUserImageUrl()).isEqualTo("http://image");
    }
}

