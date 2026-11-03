import { userApiClient } from '@/shared/api/client';
import type {
  ApiResponse,
  UserProfile,
  UserDetail,
  FilterParams,
  LocationUpdateRequest,
  UserProfileStats,
} from '@/types';

export const userApi = {
  // 사용자 목록 조회
  getUsers: (page: number, size: number, filters: FilterParams) =>
    userApiClient.get<ApiResponse<UserProfile[]>>('/list', {
      params: { page, size, ...filters },
    }),

  // 사용자 상세 조회
  getUser: (userId: string) =>
    userApiClient.get<ApiResponse<UserDetail>>(`/${userId}`),

  // 사용자 조회수 기록
  recordView: (userId: string) =>
    userApiClient.post<ApiResponse<void>>(`/${userId}/view`),

  // 좋아요 수 조회
  getLikeCount: (userId: string) =>
    userApiClient.get<ApiResponse<number>>(`/${userId}/like-count`),

  // 프로필 수정
  updateProfile: (data: FormData) =>
    userApiClient.post<ApiResponse<UserDetail>>('/profile/edit', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  // 위치 업데이트
  updateLocation: (data: LocationUpdateRequest) =>
    userApiClient.patch<ApiResponse<void>>('/location', data),

  // 프로필 통계 조회
  getProfileStats: () =>
    userApiClient.get<ApiResponse<UserProfileStats>>('/profile/stats'),
};
