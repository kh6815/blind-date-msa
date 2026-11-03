import { chatApiClient } from '@/shared/api/client';
import type { ApiResponse, ChatRoom, ChatMessage, ChatRoomCreateRequest } from '@/types';

interface HasUnreadResponse {
  has_unread: boolean;
}

export const chatApi = {
  // 채팅방 목록 조회
  getRooms: (page?: number, size?: number) =>
    chatApiClient.get<ApiResponse<ChatRoom[]>>('/rooms', {
      params: { page: page || 0, size: size || 20 },
    }),

  // 채팅방 생성
  createRoom: (data: ChatRoomCreateRequest) =>
    chatApiClient.post<ApiResponse<ChatRoom>>('/rooms', data),

  // 채팅방 단건 조회
  getRoom: (roomId: string) =>
    chatApiClient.get<ApiResponse<ChatRoom>>(`/rooms/${roomId}`),

  // 채팅 메시지 조회
  getMessages: (roomId: string, page: number, size: number) =>
    chatApiClient.get<ApiResponse<ChatMessage[]>>(`/rooms/${roomId}/messages`, {
      params: { page, size },
    }),

  // 메시지 읽음 처리
  markMessagesAsRead: (roomId: string) =>
    chatApiClient.put<ApiResponse<any>>(`/rooms/${roomId}/messages/read`),

  // 읽지 않은 메시지 존재 여부
  hasUnreadMessages: () =>
    chatApiClient.get<ApiResponse<HasUnreadResponse>>('/has-unread'),
};
