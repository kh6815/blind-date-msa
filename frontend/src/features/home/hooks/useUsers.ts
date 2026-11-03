import { useQuery, useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/features/home/api/userApi';
import type { FilterParams, LocationUpdateRequest, UserProfileStats } from '@/types';

// 사용자 목록 조회 (무한 스크롤)
export const useUsers = (filters: FilterParams) => {
  return useInfiniteQuery({
    queryKey: ['users', filters],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await userApi.getUsers(pageParam, 20, filters);
      return {
        data: response.data.data || [],
        nextPage: response.data.data.length === 20 ? pageParam + 1 : undefined,
      };
    },
    getNextPageParam: (lastPage) => lastPage.nextPage,
    initialPageParam: 0,
  });
};

// 사용자 상세 조회
export const useUser = (userId: string) => {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: async () => {
      const response = await userApi.getUser(userId);
      return response.data.data;
    },
    enabled: !!userId,
  });
};

// 사용자 조회수 기록 (React Query 캐싱으로 중복 호출 방지)
export const useRecordView = (userId: string) => {
  return useQuery({
    queryKey: ['recordView', userId],
    queryFn: async () => {
      await userApi.recordView(userId);
      return true;
    },
    enabled: !!userId,
    staleTime: Infinity, // 한 번 성공하면 다시 호출하지 않음
    gcTime: Infinity, // 캐시를 브라우저 세션 동안 유지
    retry: 2, // 네트워크 오류 시 최대 2번 재시도
  });
};

// 좋아요 수 조회
export const useLikeCount = (userId: string) => {
  return useQuery({
    queryKey: ['likeCount', userId],
    queryFn: async () => {
      const response = await userApi.getLikeCount(userId);
      return response.data.data || 0;
    },
    enabled: !!userId,
  });
};

// 프로필 수정
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (formData: FormData) => {
      const response = await userApi.updateProfile(formData);
      return response.data.data;
    },
    onSuccess: () => {
      // 현재 사용자 정보 갱신
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
      queryClient.invalidateQueries({ queryKey: ['profileStats'] });
    },
  });
};

// 위치 업데이트
export const useUpdateLocation = () => {
  return useMutation({
    mutationFn: async (data: LocationUpdateRequest) => {
      await userApi.updateLocation(data);
    },
  });
};

// 프로필 통계 조회
export const useProfileStats = () => {
  return useQuery({
    queryKey: ['profileStats'],
    queryFn: async (): Promise<UserProfileStats> => {
      const response = await userApi.getProfileStats();
      return response.data.data;
    },
  });
};
