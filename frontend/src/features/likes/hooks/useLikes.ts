import { useMutation, useInfiniteQuery } from '@tanstack/react-query';
import { likesApi } from '@/features/likes/api/likesApi';

// 좋아요한 사용자 목록 조회 (무한 스크롤)
export const useLikedUsers = () => {
  return useInfiniteQuery({
    queryKey: ['likedUsers'],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await likesApi.getLikedUsers(pageParam, 20);
      const data = response.data.data || [];
      return {
        data,
        nextPage: data.length === 20 ? pageParam + 1 : undefined,
      };
    },
    getNextPageParam: (lastPage) => lastPage.nextPage,
    initialPageParam: 0,
  });
};

// 좋아요 읽음 처리
export const useMarkLikesAsRead = () => {
  return useMutation({
    mutationFn: async () => {
      const response = await likesApi.markAsRead();
      return response.data;
    },
    onError: (error) => {
      console.error('Failed to mark likes as read:', error);
    },
  });
};
