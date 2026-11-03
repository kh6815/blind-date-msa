// 좋아요 정보
export interface Like {
  id: number;
  userId: number;
  targetUserId: number;
  createdAt: string;
}

// 좋아요한 사용자 정보 (API 응답)
export interface LikedUser {
  user_id: number;
  nickname: string;
  profile_image_url?: string;
  liked_at: string;
  is_read: boolean;
}

// 좋아요 추가 요청
export interface AddLikeRequest {
  target_user_id: number;
}

// 좋아요 확인 응답
export interface LikeCheckResponse {
  isLiked: boolean;
}

// 좋아요 수 응답
export interface LikeCountResponse {
  count: number;
}
