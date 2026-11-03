// 공통 API 응답 타입
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
