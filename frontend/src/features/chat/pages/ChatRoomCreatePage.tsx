import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '@/features/chat/api/chatApi';
import { useAuthStore } from '@/store/useAuthStore';
import { useCreateChatRoom } from '@/features/chat/hooks/useChatRooms';
import type { ChatRoomCreateRequest } from '@/types/chat';

function ChatRoomCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const targetUserId = searchParams.get('targetUserId');
  const createRoomMutation = useCreateChatRoom();

  useEffect(() => {
    if (!user || !targetUserId) {
      navigate('/');
      return;
    }

    const request: ChatRoomCreateRequest = {
      participant_user_ids: [Number(targetUserId)], // 서버에서 현재 사용자 ID를 자동으로 추가
    };

    // Use mutation with onSuccess/onError callbacks
    createRoomMutation.mutate(request, {
      onSuccess: async (createdRoom) => {
        const roomId = createdRoom.id;

        // Thymeleaf처럼 생성 후 다시 GET으로 room 정보를 가져옴 (targetUser 정보가 올바르게 채워짐)
        const getRoomResponse = await chatApi.getRoom(roomId.toString());
        const room = getRoomResponse.data.data;

        // 채팅방 페이지로 이동 (올바른 room 정보 전달)
        navigate(`/chats/room/${roomId}`, { state: { room } });
      },
      onError: () => {
        navigate('/');
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, targetUserId, navigate]);

  return (
    <div className="flex justify-center items-center h-full">
      <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
    </div>
  );
}

export default ChatRoomCreatePage;
