import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useChatRooms } from '@/features/chat/hooks/useChatRooms.ts';
import { useChatListSocket } from '@/features/chat/hooks/useChatListSocket.ts';
import { formatRelativeTime } from '@/features/chat/util/DataFormatter.ts';
import type { ChatRoom } from '@/types/chat';

//TODO 새로운 채팅방을 실시간으로 반영하고 싶다면:
//   1. 백엔드 개선: 새 채팅방 생성 시 /topic/users/{userId}/new-chat 같은 개인 토픽으로 알림 발행
//   2. 프론트 개선: 해당 토픽을 구독하고, 알림 받으면 채팅방 목록 refetch
function ChatListPage() {
  const pageSize = 20;
  const [page, setPage] = useState(0);

  // 채팅방 목록 조회
  const { data: initialRooms, isLoading } = useChatRooms(page, pageSize);

  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [hasMore, setHasMore] = useState(true);

    // WebSocket 연결 및 실시간 업데이트
    useChatListSocket(rooms, setRooms);

  // 타임스탬프 자동 갱신 (1분마다)
  const [currentTime, setCurrentTime] = useState(new Date());

  // 더 보기 버튼 클릭 핸들러
  const handleLoadMore = () => {
    setPage(prev => prev + 1);
  };

  // 초기 데이터 동기화
  useEffect(() => {
    if (!initialRooms) return;

    setRooms((prevRooms) => {
      if (page === 0) {
        // 첫 페이지: 기존 데이터와 병합 (WebSocket 업데이트 유지)
        // initialRooms를 기준으로 하되, 기존에 있던 방은 업데이트
        return initialRooms.map((newRoom) => {
          const existingRoom = prevRooms.find((r) => r.id === newRoom.id);
          if (existingRoom) {
            // 기존 방이 있으면 최신 데이터로 업데이트 (WebSocket 업데이트는 유지)
            // last_message_at이 더 최신인 것을 사용
            const existingTime = new Date(existingRoom.last_message_at || 0).getTime();
            const newTime = new Date(newRoom.last_message_at || 0).getTime();

            return existingTime > newTime ? existingRoom : newRoom;
          }
          return newRoom;
        });
      } else {
        // 추가 페이지: 새로운 방만 추가
        const newRoomIds = new Set(prevRooms.map((r) => r.id));
        const roomsToAdd = initialRooms.filter((r) => !newRoomIds.has(r.id));
        return [...prevRooms, ...roomsToAdd];
      }
    });

    // 더 이상 데이터가 없으면 hasMore = false
    if (initialRooms.length < pageSize) {
      setHasMore(false);
    }
  }, [initialRooms, page, pageSize]);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000); // 60초마다 갱신

    return () => clearInterval(timer);
  }, []);

  if (isLoading && page === 0) {
    return (
      <div className="flex items-center justify-center p-10">
        <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
      </div>
    );
  }

  return (
    <div className="pb-20">
      {!rooms || rooms.length === 0 ? (
        <div className="p-[15px_20px] text-center text-gray-500">
          <i className="fas fa-comments text-5xl text-gray-500 mb-3"></i>
          <p>참여 중인 채팅방이 없습니다.</p>
          <Link
            to="/"
            className="inline-flex items-center gap-2 mt-4 text-accent-color no-underline hover:underline"
          >
            <i className="fas fa-search"></i> 친구 찾으러 가기
          </Link>
        </div>
      ) : (
        <>
          <div>
            {rooms.map((room) => (
              <Link
                key={room.id}
                to={`/chats/room/${room.id}`}
                state={{ room }}
                className="flex items-center p-[15px_20px] border-b border-[#e9ecef] no-underline hover:bg-gray-50 relative"
              >
                {/* 프로필 이미지 */}
                {room.target_user_image_url ? (
                  <div
                    className="w-[60px] h-[60px] rounded-full bg-gray-200 mr-[15px] bg-cover bg-center flex-shrink-0"
                    style={{
                      backgroundImage: `url(${room.target_user_image_url})`,
                    }}
                  ></div>
                ) : (
                  <div className="w-[60px] h-[60px] rounded-full bg-accent-color mr-[15px] flex items-center justify-center text-white font-bold text-xl flex-shrink-0">
                    {room.target_user_nickname ? room.target_user_nickname.charAt(0).toUpperCase() : '?'}
                  </div>
                )}

                {/* 정보 영역 */}
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-base mb-[5px] text-text-primary">
                    {room.target_user_nickname || '알 수 없음'}
                  </div>
                  <div className="text-text-secondary text-sm whitespace-nowrap overflow-hidden text-ellipsis">
                    {room.last_message_preview || '메시지가 없습니다'}
                  </div>
                </div>

                {/* 오른쪽 영역 (시간 + 읽지 않은 메시지 배지) */}
                <div className="flex flex-col items-end gap-2 ml-2">
                  <span className="text-xs text-text-secondary whitespace-nowrap">
                    {formatRelativeTime(room.last_message_at, currentTime)}
                  </span>
                  {room.unread_message_count && room.unread_message_count > 0 && (
                    <div className="min-w-[24px] h-6 bg-red-500 rounded-full flex items-center justify-center px-2">
                      <span className="text-white text-xs font-bold">
                        {room.unread_message_count >= 10 ? '10+' : room.unread_message_count === 0 ? '' : room.unread_message_count }
                      </span>
                    </div>
                  )}
                </div>
              </Link>
            ))}
          </div>

          {/* 더 보기 버튼 */}
          {hasMore && (
            <div className="p-4 text-center">
              <button
                onClick={handleLoadMore}
                disabled={isLoading}
                className="px-6 py-2 bg-accent-color text-white rounded-lg hover:bg-opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? '로딩 중...' : '더 보기'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default ChatListPage;