import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '@/features/profile/api/profileApi';

// 프로필 통계 조회
export const useProfileStats = () => {
  return useQuery({
    queryKey: ['profileStats'],
    queryFn: async () => {
      const response = await profileApi.getStats();
      return response.data.data;
    },
  });
};

// 프로필 수정
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (formData: FormData) => {
      const response = await profileApi.editProfile(formData);
      return response.data.data;
    },
    onSuccess: () => {
      // 프로필 관련 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: ['profileStats'] });
    },
    onError: (error) => {
      console.error('Failed to update profile:', error);
    },
  });
};

// 위치 업데이트
export const useUpdateLocation = () => {
  return useMutation({
    mutationFn: async ({ latitude, longitude }: { latitude: number; longitude: number }) => {
      const response = await profileApi.updateLocation(latitude, longitude);
      return response.data;
    },
    onError: (error) => {
      console.error('Failed to update location:', error);
    },
  });
};
