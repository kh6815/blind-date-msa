import { useMutation } from '@tanstack/react-query';
import { authApi } from '@/features/auth/api/authApi';
import type { LoginResponse, SignupRequest } from '@/types';

interface LoginRequestData {
  email: string;
  password: string;
}

// 로그인 Hook
export const useLogin = () => {
  return useMutation({
    mutationFn: async (data: LoginRequestData): Promise<LoginResponse> => {
      const response = await authApi.login(data);
      return response.data.data;
    },
  });
};

// 로그아웃 Hook
export const useLogout = () => {
  return useMutation({
    mutationFn: async (): Promise<void> => {
      await authApi.logout();
    },
  });
};

// 회원가입 Hook
export const useSignup = () => {
  return useMutation({
    mutationFn: async (data: SignupRequest): Promise<LoginResponse> => {
      const response = await authApi.signup(data);
      return response.data.data;
    },
  });
};
