import { Link } from 'react-router-dom';
import { useChatRooms } from '@/features/chat/hooks/useChatRooms';

function ChatListPage() {
  const { data: rooms, isLoading } = useChatRooms();

  const formatTime = (dateString: string | undefined) => {
    if (!dateString) return '';

    const date = new Date(dateString);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return '방금 전';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}분 전`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}시간 전`;
    if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}일 전`;

    return `${date.getMonth() + 1}월 ${date.getDate()}일`;
  };

  if (isLoading) {
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
                  {formatTime(room.last_message_at)}
                </span>
                {room.unread_message_count && room.unread_message_count > 0 && (
                  <div className="min-w-[24px] h-6 bg-red-500 rounded-full flex items-center justify-center px-2">
                    <span className="text-white text-xs font-bold">
                      {room.unread_message_count >= 10 ? '9+' : room.unread_message_count}
                    </span>
                  </div>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

export default ChatListPage;