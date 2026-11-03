import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import { useCreateChatRoom, useChatRoom } from '@/features/chat/hooks/useChatRooms.ts';
import type { ChatRoomCreateRequest } from '@/types/chat';

function ChatRoomCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const targetUserId = searchParams.get('targetUserId');
  const createRoomMutation = useCreateChatRoom();
  const [createdRoomId, setCreatedRoomId] = useState<string | null>(null);

  // 생성된 채팅방 정보 조회
  const { data: createdRoomInfo } = useChatRoom(createdRoomId || '');

  useEffect(() => {
    if (!user || !targetUserId) {
      navigate('/');
      return;
    }

    const request: ChatRoomCreateRequest = {
      participant_user_ids: [Number(targetUserId)],
    };

    createRoomMutation.mutate(request, {
      onSuccess: (createdRoom) => {
        setCreatedRoomId(createdRoom.id.toString());
      },
      onError: () => {
        navigate('/');
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, targetUserId, navigate]);

  // 채팅방 정보를 가져온 후 페이지 이동
  useEffect(() => {
    if (createdRoomInfo && createdRoomId) {
      navigate(`/chats/room/${createdRoomId}`, { state: { room: createdRoomInfo } });
    }
  }, [createdRoomInfo, createdRoomId, navigate]);

  return (
    <div className="flex justify-center items-center h-full">
      <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
    </div>
  );
}

export default ChatRoomCreatePage;
