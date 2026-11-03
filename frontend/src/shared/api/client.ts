import axios from 'axios';
import { useAuthStore } from '@/store/useAuthStore';

const userApiClient = axios.create({
  baseURL: import.meta.env.VITE_USER_API_URL || '/api/v1/users',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

const chatApiClient = axios.create({
  baseURL: import.meta.env.VITE_CHAT_API_URL || '/api/v1/chats',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

userApiClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

chatApiClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

userApiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // Clear auth state
      const { logout } = useAuthStore.getState();
      logout();

      // Show alert and redirect
      alert('로그인이 필요합니다.');
      window.location.href = '/login';
    } else if (status === 404) {
      // 리소스를 찾을 수 없음
      console.error('요청한 리소스를 찾을 수 없습니다:', error.config?.url);
      alert('요청한 정보를 찾을 수 없습니다.');
      window.location.href = '/';
    } else if (status === 403) {
      // 권한 없음 - 401과 동일하게 처리
      const { logout } = useAuthStore.getState();
      logout();
      alert('접근 권한이 없습니다. 다시 로그인해주세요.');
      window.location.href = '/login';
    } else if (status >= 500) {
      // 서버 에러
      console.error('서버 오류:', error);
      alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }

    return Promise.reject(error);
  }
);

chatApiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // Clear auth state
      const { logout } = useAuthStore.getState();
      logout();

      // Show alert and redirect
      alert('로그인이 필요합니다.');
      window.location.href = '/login';
    } else if (status === 404) {
      // 리소스를 찾을 수 없음
      console.error('요청한 리소스를 찾을 수 없습니다:', error.config?.url);
      alert('요청한 정보를 찾을 수 없습니다.');
      window.location.href = '/chats';
    } else if (status === 403) {
      // 권한 없음 - 401과 동일하게 처리
      const { logout } = useAuthStore.getState();
      logout();
      alert('접근 권한이 없습니다. 다시 로그인해주세요.');
      window.location.href = '/login';
    } else if (status >= 500) {
      // 서버 에러
      console.error('서버 오류:', error);
      alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }

    return Promise.reject(error);
  }
);

export { userApiClient, chatApiClient };