import { userApiClient } from '@/shared/api/client';
import type { ApiResponse, UserResponse, UserProfileStats } from '@/types';

export const profileApi = {
  // 현재 사용자 정보 조회
  getMe: () =>
    userApiClient.get<ApiResponse<UserResponse>>('/me'),

  // 프로필 통계 조회
  getStats: () =>
    userApiClient.get<ApiResponse<UserProfileStats>>('/profile/stats'),

  // 위치 업데이트
  updateLocation: (latitude: number, longitude: number) =>
    userApiClient.patch('/location', { latitude, longitude }),

  // 프로필 수정
  editProfile: (formData: FormData) =>
    userApiClient.post<ApiResponse<UserResponse>>('/profile/edit', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
};
