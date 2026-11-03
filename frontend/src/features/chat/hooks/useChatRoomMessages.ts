import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { useInfiniteChatMessages } from './useChatRooms';
import { useChatRoomSocket } from './useChatRoomSocket';
import type { ChatMessage } from '@/types/chat';

/**
 * 채팅방 메시지 통합 관리 Hook
 * - 무한 스크롤 메시지 조회
 * - WebSocket 실시간 메시지 수신
 * - 읽음 처리 업데이트
 * - 스크롤 위치 관리
 */
export function useChatRoomMessages(roomId: string, pageSize: number = 50) {
  // 무한 스크롤 메시지 조회
  const {
    data: infiniteData,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteChatMessages(roomId, pageSize);

  // React Query의 pages를 평탄화하여 메시지 배열로 변환
  const serverMessages = useMemo(() => {
    if (!infiniteData) return [];
    return infiniteData.pages.flat();
  }, [infiniteData]);

  // 로컬 메시지 상태 (읽음 처리 업데이트용)
  const [localMessages, setLocalMessages] = useState<ChatMessage[]>([]);

  // WebSocket 실시간 메시지
  const { realtimeMessages, isConnected, sendMessage } = useChatRoomSocket(
    roomId,
    setLocalMessages
  );

  // 전체 메시지 = 서버 메시지 + 로컬 업데이트 + 실시간 메시지
  const allMessages = useMemo(() => {
    // serverMessages에서 업데이트된 메시지를 localMessages로 덮어쓰기
    const mergedMessages = serverMessages.map((msg) => {
      const localUpdate = localMessages.find((local) => local.id === msg.id);
      return localUpdate || msg;
    });
    return [...mergedMessages, ...realtimeMessages];
  }, [serverMessages, localMessages, realtimeMessages]);

  // 스크롤 관련 ref
  const messagesAreaRef = useRef<HTMLDivElement>(null);

  // 이전 메시지 로드 (무한 스크롤)
  const loadMoreMessages = useCallback(async () => {
    if (!hasNextPage || isFetchingNextPage) return;

    const previousScrollHeight = messagesAreaRef.current?.scrollHeight || 0;

    await fetchNextPage();

    // 스크롤 위치 유지
    setTimeout(() => {
      if (messagesAreaRef.current) {
        const newScrollHeight = messagesAreaRef.current.scrollHeight;
        messagesAreaRef.current.scrollTop = newScrollHeight - previousScrollHeight;
      }
    }, 0);
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  // 스크롤 이벤트 핸들러
  useEffect(() => {
    const messagesArea = messagesAreaRef.current;
    if (!messagesArea) return;

    const handleScroll = () => {
      if (messagesArea.scrollTop === 0 && hasNextPage && !isFetchingNextPage) {
        loadMoreMessages();
      }
    };

    messagesArea.addEventListener('scroll', handleScroll);
    return () => messagesArea.removeEventListener('scroll', handleScroll);
  }, [hasNextPage, isFetchingNextPage, loadMoreMessages]);

  return {
    // 메시지 데이터
    messages: allMessages,
    isLoading: isFetchingNextPage,

    // WebSocket
    isConnected,
    sendMessage,

    // 스크롤 관련
    messagesAreaRef,
    loadMoreMessages,
    hasNextPage,
  };
}