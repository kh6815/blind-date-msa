import { userApiClient } from '@/shared/api/client';
import type { ApiResponse } from '@/types';

interface LikeCheckResponse {
  is_liked: boolean;
  like_count: number;
}

interface HasUnreadResponse {
  has_unread: boolean;
}

export const likeApi = {
  // 좋아요 여부 확인
  check: (targetUserId: number) =>
    userApiClient.post<ApiResponse<LikeCheckResponse>>('/likes/check', {
      target_user_id: targetUserId,
    }),

  // 좋아요 추가
  add: (targetUserId: number) =>
    userApiClient.post<ApiResponse<void>>('/likes', { target_user_id: targetUserId }),

  // 좋아요 삭제
  remove: (targetUserId: number) =>
    userApiClient.delete<ApiResponse<void>>('/likes', {
      data: { target_user_id: targetUserId },
    }),

  // 미읽은 좋아요 알림 여부
  hasUnread: () =>
    userApiClient.get<ApiResponse<HasUnreadResponse>>('/likes/has-unread'),
};
