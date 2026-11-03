import { useEffect, useState, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/useAuthStore';
import { useMarkMessagesAsRead } from './useChatRooms.ts';
import type { ChatMessage } from '@/types/chat';

/**
 * 채팅방 WebSocket 연결 및 메시지 관리 Hook
 * - WebSocket 연결 및 메시지/읽음 이벤트 구독
 * - 실시간 메시지를 realtimeMessages state로 관리
 * - 서버에서 로드한 메시지는 setLocalMessages로 직접 업데이트
 */
export function useChatRoomSocket(
  roomId: string,
  setLocalMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>
) {
  const user = useAuthStore((state) => state.user);
  const markMessagesAsReadMutation = useMarkMessagesAsRead();

  const [realtimeMessages, setRealtimeMessages] = useState<ChatMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const stompClient = useRef<Client | null>(null);

  // 읽음 이벤트 처리 (Timestamp 기반)
  const handleReadEvent = useCallback((readEvent: { user_id: number; read_at: string }) => {
    console.log('[ChatRoom] Read event received:', readEvent);

    // 다른 사람이 읽은 경우에만 처리
    if (!user || readEvent.user_id === user.id) return;

    const readAtTimestamp = new Date(readEvent.read_at);

    // 1. 실시간 메시지의 unread_count 업데이트
    setRealtimeMessages((prevMessages) =>
      prevMessages.map((msg) => {
        if (msg.sender_user_id !== user.id) return msg;

        const sentAtTimestamp = new Date(msg.sent_at);
        if (sentAtTimestamp <= readAtTimestamp && msg.unread_count && msg.unread_count > 0) {
          return {
            ...msg,
            unread_count: Math.max(0, msg.unread_count - 1),
          };
        }
        return msg;
      })
    );

    // 2. 서버에서 로드한 메시지의 unread_count 업데이트
    setLocalMessages((prevMessages) =>
      prevMessages.map((msg) => {
        if (msg.sender_user_id !== user.id) return msg;

        const sentAtTimestamp = new Date(msg.sent_at);
        if (sentAtTimestamp <= readAtTimestamp && msg.unread_count && msg.unread_count > 0) {
          return {
            ...msg,
            unread_count: Math.max(0, msg.unread_count - 1),
          };
        }
        return msg;
      })
    );

    console.log('[ChatRoom] Updated message(s) unread count');
  }, [user, setLocalMessages]);

  // 읽음 처리
  const markAllAsReadRef = useRef<(() => void) | null>(null);
  markAllAsReadRef.current = () => {
    if (!roomId || !user) return;
    console.log('[ChatRoom] Marking messages as read for room:', roomId);
    markMessagesAsReadMutation.mutate(roomId);
  };

  // 메시지 전송
  const sendMessage = useCallback(
    (content: string) => {
      if (!content.trim() || !stompClient.current || !isConnected || !user) {
        return;
      }

      stompClient.current.publish({
        destination: `/pub/chats/${roomId}`,
        body: JSON.stringify({
          sender_user_id: user.id,
          content: content.trim(),
          type: 'TEXT',
        }),
      });
    },
    [roomId, user, isConnected]
  );

  // WebSocket 연결
  useEffect(() => {
    if (!roomId || !user) {
      console.log('[ChatRoom] Skipping WebSocket - no roomId or user');
      return;
    }

    console.log('[ChatRoom] Setting up WebSocket connection for room:', roomId);

    const socket = new SockJS(`${window.location.protocol}//${window.location.host}/ws/chat`);
    const client = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {},
      onConnect: () => {
        console.log('[ChatRoom] WebSocket connected');
        setIsConnected(true);

        // 메시지 구독
        client.subscribe(`/topic/chats/${roomId}`, (message) => {
          console.log('[ChatRoom] Message received:', message.body);
          const newMessage: ChatMessage = JSON.parse(message.body);
          setRealtimeMessages((prev) => [...prev, newMessage]);

          // 다른 사람이 보낸 메시지는 자동으로 읽음 처리
          if (newMessage.sender_user_id !== user.id) {
            markAllAsReadRef.current?.();
          }
        });

        // 읽음 이벤트 구독
        client.subscribe(`/topic/chats/${roomId}/read`, (message) => {
          console.log('[ChatRoom] Read event received:', message.body);
          const readEvent = JSON.parse(message.body);
          handleReadEvent(readEvent);
        });

        // 채팅방 진입 시 자동 읽음 처리
        markAllAsReadRef.current?.();
      },
      onStompError: (frame) => {
        console.error('[ChatRoom] STOMP error:', frame.headers['message']);
        console.error('[ChatRoom] Additional details:', frame.body);
      },
      onWebSocketClose: () => {
        console.log('[ChatRoom] WebSocket disconnected');
        setIsConnected(false);
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      console.log('[ChatRoom] Cleaning up - deactivating WebSocket');
      client.deactivate();
    };
  }, [roomId, user, handleReadEvent]);

  // 외부에서 호출할 수 있는 wrapper 함수
  const markAllAsRead = useCallback(() => {
    markAllAsReadRef.current?.();
  }, []);

  return {
    realtimeMessages,
    isConnected,
    sendMessage,
    markAllAsRead,
  };
}