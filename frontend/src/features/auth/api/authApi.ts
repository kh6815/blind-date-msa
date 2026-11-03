import { userApiClient } from '@/shared/api/client';
import type { ApiResponse, LoginResponse, SignupRequest } from '@/types';

interface LoginRequest {
  email: string;
  password: string;
}

export const authApi = {
  // 로그인
  login: (data: LoginRequest) =>
    userApiClient.post<ApiResponse<LoginResponse>>('/auth/login', data),

  // 로그아웃
  logout: () =>
    userApiClient.post<ApiResponse<void>>('/auth/logout'),

  // 회원가입
  signup: (data: SignupRequest) =>
    userApiClient.post<ApiResponse<LoginResponse>>('', data),
};
