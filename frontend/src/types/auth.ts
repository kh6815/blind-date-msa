// 로그인 요청
export interface LoginRequest {
  email: string;
  password: string;
}

// 로그인 응답
export interface LoginResponse {
  id: number;
  email: string;
  nickname: string;
  age: number;
  gender: string;
  job?: string;
  description?: string;
  mbti?: string;
  interests?: string;
  location?: string;
  profile_image_url?: string;
  image_urls?: string[];
}

// 인증 상태
export interface AuthState {
  isAuthenticated: boolean;
  user: LoginResponse | null;
  login: (user: LoginResponse) => void;
  logout: () => void;
}
