import { useRef, useEffect, useCallback } from 'react';

interface UseChatScrollOptions {
  /**
   * 메시지 배열
   */
  messages: any[];

  /**
   * 이전 메시지 로드 중 여부
   */
  isLoadingMore?: boolean;

  /**
   * 더 로드할 메시지가 있는지 여부
   */
  hasMore?: boolean;

  /**
   * 이전 메시지 로드 함수
   */
  onLoadMore?: () => void;
}

/**
 * 채팅 스크롤 관리 Hook
 * - 첫 로드 시 하단 스크롤
 * - 새 메시지 추가 시 하단 스크롤
 * - 무한 스크롤 (상단 도달 시 이전 메시지 로드 + 스크롤 위치 유지)
 */
export function useChatScroll({
  messages,
  isLoadingMore = false,
  hasMore = false,
  onLoadMore,
}: UseChatScrollOptions) {
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const previousMessageCountRef = useRef(0);
  const previousScrollHeightRef = useRef(0);

  /**
   * 하단으로 스크롤
   */
  const scrollToBottom = useCallback(() => {
    if (!scrollContainerRef.current) return;

    requestAnimationFrame(() => {
      if (scrollContainerRef.current) {
        scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
      }
    });
  }, []);

  /**
   * 스크롤 위치 유지 (이전 메시지 로드 후)
   */
  const maintainScrollPosition = useCallback(() => {
    if (!scrollContainerRef.current) return;

    requestAnimationFrame(() => {
      if (scrollContainerRef.current) {
        const newScrollHeight = scrollContainerRef.current.scrollHeight;
        const scrollDiff = newScrollHeight - previousScrollHeightRef.current;
        scrollContainerRef.current.scrollTop += scrollDiff;
      }
    });
  }, []);

  /**
   * 무한 스크롤 이벤트 핸들러
   */
  const handleScroll = useCallback(() => {
    if (!scrollContainerRef.current || !onLoadMore) return;

    // 상단 도달 시 이전 메시지 로드
    if (
      scrollContainerRef.current.scrollTop === 0 &&
      hasMore &&
      !isLoadingMore
    ) {
      previousScrollHeightRef.current = scrollContainerRef.current.scrollHeight;
      onLoadMore();
    }
  }, [hasMore, isLoadingMore, onLoadMore]);

  /**
   * 스크롤 이벤트 리스너 등록
   */
  useEffect(() => {
    const scrollContainer = scrollContainerRef.current;
    if (!scrollContainer || !onLoadMore) return;

    scrollContainer.addEventListener('scroll', handleScroll);
    return () => scrollContainer.removeEventListener('scroll', handleScroll);
  }, [handleScroll, onLoadMore]);

  /**
   * 메시지 변경 시 스크롤 처리
   */
  useEffect(() => {
    if (messages.length === 0) return;

    const currentMessageCount = messages.length;
    const messageAdded = currentMessageCount > previousMessageCountRef.current;

    if (messageAdded) {
      if (isLoadingMore) {
        // 이전 메시지 로드 완료: 스크롤 위치 유지
        maintainScrollPosition();
      } else {
        // 그 외 모든 경우: 하단으로 스크롤 (첫 로드, 새 메시지 등)
        scrollToBottom();
      }
    }

    previousMessageCountRef.current = currentMessageCount;
  }, [messages.length, isLoadingMore, scrollToBottom, maintainScrollPosition]);

  return {
    scrollContainerRef,
    scrollToBottom,
  };
}