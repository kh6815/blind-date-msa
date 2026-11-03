import { useQuery } from '@tanstack/react-query';
import { chatApi } from '@/features/chat/api/chatApi';

// 채팅 메시지 조회
export const useChatMessages = (roomId: string, page: number = 0, size: number = 50) => {
  return useQuery({
    queryKey: ['chatMessages', roomId, page],
    queryFn: async () => {
      const response = await chatApi.getMessages(roomId, page, size);
      return response.data.data || [];
    },
    enabled: !!roomId,
  });
};
