import { QueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';

// 공통 에러 처리 함수
const handleError = (error: unknown) => {
  if (error instanceof AxiosError) {
    const message = error.response?.data?.message || error.message || '요청 처리 중 오류가 발생했습니다.';
    console.error('API Error:', message, error.response?.data);
    return message;
  }
  console.error('Unknown Error:', error);
  return '알 수 없는 오류가 발생했습니다.';
};

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1, // 실패 시 1번만 재시도
      refetchOnWindowFocus: false, // 윈도우 포커스 시 자동 refetch 비활성화
      staleTime: 1000 * 60 * 5, // 5분간 캐시 유지
    },
    mutations: {
      onError: (error) => {
        handleError(error);
      },
    },
  },
});

export { handleError };
