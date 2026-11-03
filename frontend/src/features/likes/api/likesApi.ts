import { userApiClient } from '@/shared/api/client';
import type { ApiResponse, LikedUser } from '@/types';

export const likesApi = {
  // 좋아요한 사용자 목록 조회
  getLikedUsers: (page: number, size: number) =>
    userApiClient.get<ApiResponse<LikedUser[]>>('/likes/list', {
      params: { page, size },
    }),

  // 좋아요 읽음 처리
  markAsRead: () =>
    userApiClient.patch('/likes/read'),
};
