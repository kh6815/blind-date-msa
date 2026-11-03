import { useQuery } from '@tanstack/react-query';
import { profileApi } from '@/features/profile/api/profileApi';
import { useAuthStore } from '@/store/useAuthStore';
import type { UserResponse } from '@/types';

export function useCurrentUser() {
  const { setUser, logout } = useAuthStore();

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: async (): Promise<UserResponse> => {
      try {
        const response = await profileApi.getMe();
        const userData = response.data.data;
        setUser(userData);
        return userData;
      } catch (error: any) {
        if (error.response?.status === 401) {
          logout();
          throw error;
        }
        throw error;
      }
    },
    retry: false,
    staleTime: 1000 * 60 * 5, // 5분
  });
}
