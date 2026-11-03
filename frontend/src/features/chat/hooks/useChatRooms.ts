import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';
import { chatApi } from '@/features/chat/api/chatApi';
import type { ChatRoomCreateRequest } from '@/types';

// 채팅방 목록 조회
export const useChatRooms = (page?: number, size?: number) => {
  return useQuery({
    queryKey: ['chatRooms', page, size],
    queryFn: async () => {
      const response = await chatApi.getRooms(page, size);
      return response.data.data || [];
    },
  });
};

// 채팅방 생성
export const useCreateChatRoom = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: ChatRoomCreateRequest) => {
      const response = await chatApi.createRoom(data);
      return response.data.data;
    },
    onSuccess: () => {
      // 채팅방 목록 갱신
      queryClient.invalidateQueries({ queryKey: ['chatRooms'] });
    },
    onError: (error) => {
      console.error('Failed to create chat room:', error);
    },
  });
};

// 채팅방 단건 조회
export const useChatRoom = (roomId: string) => {
  return useQuery({
    queryKey: ['chatRoom', roomId],
    queryFn: async () => {
      const response = await chatApi.getRoom(roomId);
      return response.data.data;
    },
    enabled: !!roomId,
  });
};

// 읽지 않은 메시지 존재 여부
export const useHasUnreadMessages = () => {
  return useQuery({
    queryKey: ['hasUnreadMessages'],
    queryFn: async () => {
      const response = await chatApi.hasUnreadMessages();
      return response.data.data.has_unread || false;
    },
    refetchInterval: 30000, // 30초마다 자동 갱신
  });
};

// 메시지 읽음 처리
export const useMarkMessagesAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (roomId: string) => {
      const response = await chatApi.markMessagesAsRead(roomId);
      return response.data.data;
    },
    onSuccess: (_, roomId) => {
      // 읽지 않은 메시지 수 갱신
      queryClient.invalidateQueries({ queryKey: ['unreadCount', roomId] });
      queryClient.invalidateQueries({ queryKey: ['hasUnreadMessages'] });
      queryClient.invalidateQueries({ queryKey: ['chatRooms'] });
    },
    onError: (error) => {
      console.error('Failed to mark messages as read:', error);
    },
  });
};

// 무한 스크롤 채팅 메시지 조회
export const useInfiniteChatMessages = (roomId: string, size: number = 50) => {
  return useInfiniteQuery({
    queryKey: ['infiniteChatMessages', roomId],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await chatApi.getMessages(roomId, pageParam, size);
      return response.data.data || [];
    },
    getNextPageParam: (lastPage, allPages) => {
      // 마지막 페이지가 size보다 작으면 더 이상 페이지가 없음
      if (lastPage.length < size) {
        return undefined;
      }
      return allPages.length;
    },
    enabled: !!roomId,
    initialPageParam: 0,
  });
};
