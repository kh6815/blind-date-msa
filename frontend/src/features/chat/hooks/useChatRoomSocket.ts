import { useEffect, useState, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/useAuthStore';
import type { ChatMessage } from '@/types/chat';

/**
 * 채팅방 WebSocket 연결 및 메시지 관리 Hook
 * - WebSocket 연결 및 메시지/읽음 이벤트 구독
 * - 실시간 메시지를 realtimeMessages state로 관리
 * - 서버에서 로드한 메시지는 setLocalMessages로 직접 업데이트
 * - 재연결 시 누락 메시지 동기화 콜백 지원
 */
export function useChatRoomSocket(
  roomId: string,
  setLocalMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>,
  onReconnect?: () => void
) {
  const user = useAuthStore((state) => state.user);

  const [realtimeMessages, setRealtimeMessages] = useState<ChatMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const stompClient = useRef<Client | null>(null);
  const isFirstConnect = useRef(true); // 최초 연결 여부 추적

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

  /**
   * 메시지 읽음 처리
   *
   * ref 패턴을 사용하는 이유:
   * - WebSocket onConnect에서 함수를 정의하여 최신 stompClient, user, roomId 참조
   * - ref에 할당하여 Visibility API useEffect에서 호출 가능
   * - ref는 렌더링 간에 안정적으로 유지되므로 의존성 배열에 넣지 않아도 동작
   */
  const markAllAsReadRef = useRef<(() => void) | null>(null);

  // 메시지 전송
  const sendMessage = useCallback(
    (content: string, type: 'TEXT' | 'IMAGE' = 'TEXT') => {
      if (!content.trim() || !stompClient.current || !isConnected || !user) {
        return;
      }

      stompClient.current.publish({
        destination: `/pub/chats/${roomId}`,
        body: JSON.stringify({
          sender_user_id: user.id,
          content: content.trim(),
          type,
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
        const isReconnect = !isFirstConnect.current;
        console.log(`[ChatRoom] WebSocket ${isReconnect ? 'reconnected' : 'connected'}`);
        setIsConnected(true);

        // 재연결인 경우 누락된 메시지 동기화
        if (isReconnect && onReconnect) {
          console.log('[ChatRoom] Triggering reconnect sync');
          onReconnect();
        }

        isFirstConnect.current = false;

        /**
         * 읽음 처리 함수를 onConnect 내부에서 정의하는 이유:
         * - WebSocket 연결 시점의 최신 user, stompClient 참조 사용
         * - 클로저로 roomId도 접근 가능
         * - ref에 할당하여 외부(Visibility API)에서도 호출 가능
         */
        markAllAsReadRef.current = () => {
          // 연결 상태 체크
          if (!stompClient.current || !user) {
            console.log('[ChatRoom] Cannot mark as read - not connected or no user');
            return;
          }

          // 탭이 비활성화 상태면 읽음 처리 스킵 (백그라운드에서 읽음 방지)
          if (document.visibilityState !== 'visible') {
            console.log('[ChatRoom] Tab not visible - skipping read');
            return;
          }

          // WebSocket으로 읽음 처리 요청
          console.log('[ChatRoom] Marking messages as read via WebSocket for room:', roomId);
          stompClient.current.publish({
            destination: `/pub/chats/${roomId}/read`,
            body: JSON.stringify({
              user_id: user.id,
            }),
          });
        };

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
      markAllAsReadRef.current = null;
      isFirstConnect.current = true; // cleanup 시 초기화
    };
  }, [roomId, user, handleReadEvent, onReconnect]);

    // Window Visibility API - 탭 활성화 시 읽음 처리
    useEffect(() => {
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                console.log('[ChatRoom] Tab became visible - marking messages as read');
                markAllAsReadRef.current?.();
            }
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);

        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, []);

  return {
    realtimeMessages,
    isConnected,
    sendMessage,
  };
}