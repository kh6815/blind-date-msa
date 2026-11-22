import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/useAuthStore';
import type { ChatRoom } from '@/types/chat';

interface ChatRoomUpdateEvent {
  update_type: 'CREATED' | 'UPDATED' | 'DELETED';
  room_id?: string;
  chat_room?: ChatRoom;
  last_message_preview?: string;
  last_message_at?: string;
  unread_message_count?: number;
}

/**
 * 채팅 목록 WebSocket 연결 Hook (최적화 버전)
 * - 사용자별 채팅방 업데이트 토픽 단일 구독: /topic/users/{userId}/chat-updates
 * - CREATED: 새 채팅방 생성 알림
 * - UPDATED: 메시지 전송 시 마지막 메시지 정보 갱신 (O(1) Redis Pub)
 * - DELETED: 채팅방 삭제 알림
 * - 100개 채팅방이든 1000개든 **단 1개의 WebSocket 구독**으로 모든 이벤트 수신
 */
export function useChatListSocket(
    setRooms: React.Dispatch<React.SetStateAction<ChatRoom[]>>
) {
  const user = useAuthStore((state) => state.user);
  const stompClient = useRef<Client | null>(null);

  // 채팅방 업데이트 이벤트 처리 함수
  const handleChatRoomUpdate = useCallback(
    (event: ChatRoomUpdateEvent) => {

      if(event.update_type === 'CREATED' && event.room_id){
          setRooms((prevRooms) => {
              // 채팅방이 목록에 없음 → 새 채팅방 추가 (첫 메시지 전송된 경우)
              console.log('[ChatList] New chatroom detected, adding to list:', event.room_id);

              const chatRoom = event.chat_room;
              if (!event.room_id || !chatRoom) {
                  console.error('[ChatList] Invalid chat_room data, missing required fields:', chatRoom);
                  return prevRooms;
              }

              const newRoom: ChatRoom = {
                  ...chatRoom,
                  id: event.room_id,

              };
              return [newRoom, ...prevRooms];
          })
      }
      else if (event.update_type === 'UPDATED' && event.room_id) {
        // 채팅방 업데이트: 목록에 없으면 추가, 있으면 마지막 메시지 정보 갱신
        setRooms((prevRooms) => {
          // 채팅방이 목록에 있음 → 마지막 메시지 정보 갱신
          const updatedRooms = prevRooms.map((room) => {
            if (room.id !== event.room_id) return room;

            return {
              ...room,
              last_message_preview: event.last_message_preview || room.last_message_preview,
              last_message_at: event.last_message_at || room.last_message_at,
              unread_message_count: (room.unread_message_count ?? 0) + 1,
            };
          });

          // 최신 메시지 순으로 정렬
          return updatedRooms.sort((a, b) => {
            const timeA = new Date(a.last_message_at || 0).getTime();
            const timeB = new Date(b.last_message_at || 0).getTime();
            return timeB - timeA;
          });
        });
      } else if (event.update_type === 'DELETED' && event.room_id) {
        // 채팅방 삭제: 목록에서 제거
        setRooms((prevRooms) => prevRooms.filter((r) => r.id !== event.room_id));
        console.log('[ChatList] Removed chatroom from list:', event.room_id);
      }
    },
    [setRooms]
  );

  // WebSocket 연결
  useEffect(() => {
    if (!user) {
      console.log('[ChatList] Skipping WebSocket connection - no user');
      return;
    }

    console.log('[ChatList] Setting up WebSocket connection for user:', user.id);

    const socket = new SockJS(`${window.location.protocol}//${window.location.host}/ws/chat`);
    const client = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {},
      onConnect: () => {
        console.log('[ChatList] WebSocket connected');

        // 사용자별 채팅방 업데이트 토픽 단일 구독 (O(N) → O(1))
        // 채팅방 생성/업데이트/삭제 이벤트를 한 번에 수신
        console.log(`[ChatList] Subscribing to /topic/users/${user.id}/chat-updates`);
        client.subscribe(`/topic/users/${user.id}/chat-updates`, (message) => {
          console.log('[ChatList] ChatRoom update event received:', message.body);
          const event: ChatRoomUpdateEvent = JSON.parse(message.body);
          handleChatRoomUpdate(event);
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
  }, [user, handleChatRoomUpdate]);

  return {
    isConnected: !!stompClient.current?.connected,
  };
}