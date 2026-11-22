import { useState, useMemo, useCallback } from 'react';
import { useInfiniteChatMessages } from './useChatRooms';
import { useChatRoomSocket } from './useChatRoomSocket';
import type { ChatMessage } from '@/types/chat';

/**
 * 채팅방 메시지 통합 관리 Hook
 * - 무한 스크롤 메시지 조회
 * - WebSocket 실시간 메시지 수신
 * - 읽음 처리 업데이트
 * - 스크롤 위치 관리
 // * - 재연결 시 누락 메시지 동기화
 */
export function useChatRoomMessages(roomId: string, pageSize: number = 50) {
  // 무한 스크롤 메시지 조회
  const {
    data: infiniteData,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    // refetch,
  } = useInfiniteChatMessages(roomId, pageSize);

  // React Query의 pages를 평탄화하여 메시지 배열로 변환
  const serverMessages = useMemo(() => {
    if (!infiniteData) return [];
    return infiniteData.pages.flat();
  }, [infiniteData]);

  // 로컬 메시지 상태 (읽음 처리 업데이트용)
  const [localMessages, setLocalMessages] = useState<ChatMessage[]>([]);

  // 재연결 시 누락 메시지 동기화 콜백
  // const handleReconnect = useCallback(() => {
  //   console.log('[ChatRoomMessages] Syncing messages after reconnect');
  //   refetch();
  // }, [refetch]);

  // WebSocket 실시간 메시지
  const { realtimeMessages, isConnected, sendMessage } = useChatRoomSocket(
    roomId,
    setLocalMessages,
    // handleReconnect
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

  // 이전 메시지 로드 (무한 스크롤)
  const loadMoreMessages = useCallback(async () => {
    if (!hasNextPage || isFetchingNextPage) return;
    await fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  // // Window Visibility API - 포커스 복귀 시 메시지 동기화
  // useEffect(() => {
  //   const handleVisibilityChange = () => {
  //     if (document.visibilityState === 'visible') {
  //       console.log('[ChatRoomMessages] Tab became visible - syncing messages');
  //       refetch();
  //     }
  //   };
  //
  //   document.addEventListener('visibilitychange', handleVisibilityChange);
  //
  //   return () => {
  //     document.removeEventListener('visibilitychange', handleVisibilityChange);
  //   };
  // }, [refetch]);

  return {
    // 메시지 데이터
    messages: allMessages,
    isLoading: isFetchingNextPage,

    // WebSocket
    isConnected,
    sendMessage,

    // 무한 스크롤
    loadMoreMessages,
    hasNextPage,
  };
}