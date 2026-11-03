// 채팅방 정보 (백엔드 ChatRoomResponse)
export interface ChatRoom {
  id: string;
  participant_user_ids: number[];
  created_at: string;
  last_message_at?: string;
  target_user_image_url?: string;
  target_user_nickname?: string;
  is_target_user_online?: boolean;
  last_message_preview?: string;
  unread_message_count?: number;
}

// 채팅 메시지 (백엔드 ChatMessageResponse)
export interface ChatMessage {
  id: string;
  room_id: string;
  sender_user_id: number;
  type: 'TEXT' | 'IMAGE';
  content?: string;
  sent_at: string;
  read_by: Record<number, string>;
  unread_count?: number;
}

// 채팅방 생성 요청 (백엔드 ChatRoomCreateRequest)
export interface ChatRoomCreateRequest {
  participant_user_ids: number[];
}

// 채팅 메시지 전송 요청 (백엔드 ChatMessageSendRequest)
export interface ChatMessageSendRequest {
  sender_user_id: number;
  content: string;
  type?: 'TEXT' | 'IMAGE';
}
