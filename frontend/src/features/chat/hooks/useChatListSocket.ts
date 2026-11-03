import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/useAuthStore';
import type { ChatMessage, ChatRoom } from '@/types/chat';

/**
 * 채팅 목록 WebSocket 연결 Hook
 * - 모든 채팅방의 새 메시지 구독
 * - 새 메시지 수신 시 setRooms로 직접 업데이트
 * - Local State 업데이트 방식 (Thymeleaf와 동일)
 */
export function useChatListSocket(
  rooms: ChatRoom[] | undefined,
  setRooms: React.Dispatch<React.SetStateAction<ChatRoom[]>>
) {
  const user = useAuthStore((state) => state.user);
  const stompClient = useRef<Client | null>(null);

  // 새 메시지 처리 함수
  const handleNewMessage = useCallback(
    (roomId: string, message: ChatMessage) => {
      console.log('[ChatList] New message received:', { roomId, message });

      // 이미지 메시지 처리
      const messagePreview = message.type === 'IMAGE' ? '이미지' : message.content;

      setRooms((prevRooms) => {
        const roomExists = prevRooms.some((r) => r.id === roomId);

        // 기존 채팅방이 없으면 무시 (채팅방 목록을 다시 로드해야 함)
        if (!roomExists) {
          console.log('[ChatList] Room not found in list, skipping update:', roomId);
          return prevRooms;
        }

        // 업데이트된 방 목록 생성
        const updatedRooms = prevRooms.map((room) => {
          if (room.id !== roomId) return room;

          // 내가 보낸 메시지: 마지막 메시지만 업데이트
          if (message.sender_user_id === user?.id) {
            console.log('[ChatList] My message - updating last message only');
            return {
              ...room,
              last_message_preview: messagePreview,
              last_message_at: message.sent_at,
            };
          }

          // 다른 사람이 보낸 메시지: unreadCount +1 + 마지막 메시지 업데이트
          console.log('[ChatList] Other user message - incrementing unread count');
          return {
            ...room,
            last_message_preview: messagePreview,
            last_message_at: message.sent_at,
            unread_message_count: (room.unread_message_count || 0) + 1,
          };
        });

        // 최신 메시지 순으로 정렬 (last_message_at 내림차순)
        return updatedRooms.sort((a, b) => {
          const timeA = new Date(a.last_message_at || 0).getTime();
          const timeB = new Date(b.last_message_at || 0).getTime();
          return timeB - timeA; // 내림차순
        });
      });
    },
    [user?.id]
  );

  // WebSocket 연결
  useEffect(() => {
    if (!rooms || rooms.length === 0 || !user) {
      console.log('[ChatList] Skipping WebSocket connection - no rooms or user');
      return;
    }

    console.log('[ChatList] Setting up WebSocket connection for rooms:', rooms.map((r) => r.id));

    const socket = new SockJS(`${window.location.protocol}//${window.location.host}/ws/chat`);
    const client = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {},
      onConnect: () => {
        console.log('[ChatList] WebSocket connected - subscribing to rooms');

        // 모든 채팅방 구독
        rooms.forEach((room) => {
          console.log(`[ChatList] Subscribing to /topic/chats/${room.id}`);
          client.subscribe(`/topic/chats/${room.id}`, (message) => {
            console.log(`[ChatList] Message received on /topic/chats/${room.id}:`, message.body);
            const newMessage: ChatMessage = JSON.parse(message.body);
            handleNewMessage(room.id, newMessage);
          });
        });
      },
      onStompError: (frame) => {
        console.error('[ChatList] STOMP error:', frame);
      },
      onWebSocketClose: () => {
        console.log('[ChatList] WebSocket disconnected');
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      console.log('[ChatList] Cleaning up - deactivating WebSocket');
      client.deactivate();
    };
  }, [rooms, user, handleNewMessage]);

  return {
    isConnected: !!stompClient.current?.connected,
  };
}