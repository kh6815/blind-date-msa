// 사용자 기본 정보
export interface User {
  id: number;
  email: string;
  nickname: string;
  age: number;
  gender: 'MALE' | 'FEMALE';
  job?: string;
  description?: string;
  mbti?: string;
  interests?: string;
  location?: string;
  profile_image_url?: string;
  image_urls?: string[];
  is_online?: boolean;
}

// 홈페이지 프로필 카드용 (간략한 정보)
export interface UserProfile {
  id: number;
  nickname: string;
  age: number;
  gender: string;
  job?: string;
  profile_image_url?: string;
  is_online?: boolean;
  distance?: number;
}

// 사용자 상세 정보
export interface UserDetail {
  id: number;
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
  is_online?: boolean;
}

// 회원가입 요청
export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  gender: 'MALE' | 'FEMALE';
  birth_date: string;
  job?: string;
  mbti?: string;
  interests?: string;
  description?: string;
  location?: string;
}

// 프로필 수정 요청
export interface UpdateProfileRequest {
  nickname?: string;
  job?: string;
  mbti?: string;
  interests?: string;
  description?: string;
  location?: string;
}

// 위치 업데이트 요청
export interface LocationUpdateRequest {
  latitude: number;
  longitude: number;
}

// 필터 파라미터
export interface FilterParams {
  minAge?: number;
  maxAge?: number;
  gender?: string;
  mbti?: string;
  location?: string;
}

// 프로필 통계
export interface UserProfileStats {
  completeness: number;
  view_count: number;
  like_count: number;
}

// API 응답용 User 타입 (User와 동일하지만 명시적으로 export)
export type UserResponse = User;
