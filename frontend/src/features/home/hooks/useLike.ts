import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { likeApi } from '@/features/home/api/likeApi';

// 좋아요 여부 확인
export const useCheckLike = (targetUserId: string) => {
  return useQuery({
    queryKey: ['checkLike', targetUserId],
    queryFn: async () => {
      const response = await likeApi.check(Number(targetUserId));
      return response.data.data;
    },
    enabled: !!targetUserId,
  });
};

// 좋아요 추가
export const useAddLike = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (targetUserId: number) => {
      await likeApi.add(targetUserId);
    },
    onSuccess: (_, targetUserId) => {
      queryClient.invalidateQueries({ queryKey: ['checkLike', String(targetUserId)] });
      queryClient.invalidateQueries({ queryKey: ['likeCount', String(targetUserId)] });
      queryClient.invalidateQueries({ queryKey: ['hasUnreadLikes'] });
    },
    onError: (error) => {
      console.error('Failed to add like:', error);
    },
  });
};

// 좋아요 삭제
export const useRemoveLike = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (targetUserId: number) => {
      await likeApi.remove(targetUserId);
    },
    onSuccess: (_, targetUserId) => {
      queryClient.invalidateQueries({ queryKey: ['checkLike', String(targetUserId)] });
      queryClient.invalidateQueries({ queryKey: ['likeCount', String(targetUserId)] });
    },
    onError: (error) => {
      console.error('Failed to remove like:', error);
    },
  });
};

// 미읽은 좋아요 알림 여부
export const useHasUnreadLikes = () => {
  return useQuery({
    queryKey: ['hasUnreadLikes'],
    queryFn: async () => {
      const response = await likeApi.hasUnread();
      return response.data.data.has_unread || false;
    },
    refetchInterval: 30000, // 30초마다 자동 갱신
  });
};
