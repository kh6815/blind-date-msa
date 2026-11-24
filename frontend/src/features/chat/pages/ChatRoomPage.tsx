import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import { useChatRoom, useMarkMessagesAsRead, useUploadImage } from '@/features/chat/hooks/useChatRooms.ts';
import { useChatRoomMessages } from '@/features/chat/hooks/useChatRoomMessages.ts';
import { useChatScroll } from '@/features/chat/hooks/useChatScroll.ts';
import ImageModal from '@/shared/components/ImageModal';
import Loading from '@/shared/components/Loading';
import type { ChatRoom } from '@/types/chat';
import { formatDate, formatTime, getDateKey } from '@/features/chat/util/DataFormatter.ts';

function ChatRoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);

  const [messageInput, setMessageInput] = useState('');
  const [showImageModal, setShowImageModal] = useState(false);
  const [modalImageSrc, setModalImageSrc] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const roomInfo = location.state?.room as ChatRoom | null;

  // 채팅방 정보 조회
  const { data: fetchedRoomInfo } = useChatRoom(roomId || '');

  // 메시지 읽음 처리
  const markAsReadMutation = useMarkMessagesAsRead();

  // 이미지 업로드
  const uploadImageMutation = useUploadImage();

  // 현재 채팅방 정보 (location.state 우선, 없으면 API로 조회한 데이터 사용)
  const currentRoomInfo = fetchedRoomInfo || roomInfo;
  const targetUserNickname = currentRoomInfo?.target_user_nickname || '상대방';
  const targetUserImageUrl = currentRoomInfo?.target_user_image_url;
  const isTargetUserOnline = currentRoomInfo?.is_target_user_online ?? false;

  // 메시지 통합 관리 (서버 메시지 + WebSocket 실시간 메시지 + 무한 스크롤)
  const {
    messages: allMessages,
    isLoading,
    isConnected,
    sendMessage: sendChatMessage,
    loadMoreMessages,
    hasNextPage,
  } = useChatRoomMessages(roomId || '', 50);

  // 스크롤 관리
  const { scrollContainerRef, scrollToBottom } = useChatScroll({
    messages: allMessages,
    isLoadingMore: isLoading,
    hasMore: hasNextPage,
    onLoadMore: loadMoreMessages,
  });

  // 채팅방 진입 시 메시지 읽음 처리
  useEffect(() => {
    if (roomId) {
      markAsReadMutation.mutate(roomId);
    }
  }, []);

  // 메시지 전송 핸들러
  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageInput.trim()) return;

    sendChatMessage(messageInput);
    setMessageInput('');
  };

  // 파일 선택 핸들러
  const handleUploadButtonClick = () => {
    fileInputRef.current?.click();
  };

  // 파일 인풋 초기화
  const resetFileInput = () => {
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  // 이미지 업로드 핸들러
  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 파일 크기 체크 (5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('이미지는 5MB 이하만 업로드 가능합니다.');
      resetFileInput();
      return;
    }

    uploadImageMutation.mutate(file, {
      onSuccess: (imageUrl) => {
        // WebSocket으로 이미지 메시지 전송
        sendChatMessage(imageUrl, 'IMAGE');
      },
      onError: () => {
        alert('이미지 업로드에 실패했습니다.');
      },
      onSettled: () => {
        // 성공/실패 여부와 관계없이 항상 파일 인풋 초기화
        resetFileInput();
      },
    });
  };

  // 이미지 모달 열기
  const handleImageClick = (imageSrc: string) => {
    setModalImageSrc(imageSrc);
    setShowImageModal(true);
  };

  // 날짜별로 그룹화된 메시지 렌더링
  const renderMessages = () => {
    const elements: React.ReactElement[] = [];
    let lastDateKey: string | null = null;

    allMessages.forEach((message) => {
      const dateKey = getDateKey(message.sent_at);

      // 날짜 구분자 추가
      if (lastDateKey !== dateKey) {
        elements.push(
          <div key={`date-${dateKey}`} className="text-center my-4">
            <span className="inline-block bg-[#e9ecef] text-text-secondary text-xs px-3 py-1 rounded-full">
              {formatDate(message.sent_at)}
            </span>
          </div>
        );
        lastDateKey = dateKey;
      }

      const isMyMessage = message.sender_user_id === user?.id;
      const isImage = message.type === 'IMAGE';

      // 메시지 렌더링
      elements.push(
        <div
          key={message.id}
          className={`mb-3 flex items-end ${isMyMessage ? 'justify-end' : 'justify-start'}`}
        >
          {isMyMessage && message.unread_count !== undefined && message.unread_count > 0 && (
            <span className="text-xs text-yellow-500 mr-2">{message.unread_count}</span>
          )}
          <div className={`flex flex-col ${isMyMessage ? 'items-end' : 'items-start'}`}>
            <div
              className={`${isImage ? 'max-w-[250px]' : 'max-w-[250px] px-4 py-2'} rounded-2xl ${
                isMyMessage
                  ? 'bg-accent-color text-[#000]'
                  : 'bg-[#e9ecef] text-text-primary'
              }`}
            >
              {isImage ? (
                <img
                  src={message.content}
                  alt="Chat image"
                  className="max-w-full rounded-2xl cursor-zoom-in"
                  onClick={() => handleImageClick(message.content || '')}
                  onLoad={() => scrollToBottom()}
                />
              ) : (
                <div className="break-words">{message.content}</div>
              )}
            </div>
            <div className={`text-xs mt-1 ${isMyMessage ? 'text-gray-500' : 'text-gray-500'}`}>
              {formatTime(message.sent_at)}
            </div>
          </div>
        </div>
      );
    });

    return elements;
  };

  if (!user) {
    return null;
  }

  return (
    <div className="flex flex-col h-screen bg-[#f8f9fa]">
      {/* Header */}
      <header className="px-4 py-3 flex items-center bg-[#f8f9fa] border-b border-[#e9ecef]">
        <button onClick={() => navigate('/chats')} className="text-text-primary text-xl mr-3">
          <i className="fas fa-chevron-left"></i>
        </button>
        <div
          className="flex items-center flex-1 cursor-pointer"
          onClick={() => {
            const targetUserId = currentRoomInfo?.participant_user_ids?.find(id => id !== user?.id);
            if (targetUserId) {
              navigate(`/users/${targetUserId}`);
            }
          }}
        >
          {targetUserImageUrl ? (
            <div
              className="w-10 h-10 rounded-full bg-gray-200 mr-3 bg-cover bg-center"
              style={{ backgroundImage: `url(${targetUserImageUrl})` }}
            />
          ) : (
            <div className="w-10 h-10 rounded-full bg-accent-color mr-3 flex items-center justify-center text-white font-bold">
              {targetUserNickname.charAt(0).toUpperCase()}
            </div>
          )}
          <div className="flex-1">
            <div className="text-text-primary font-bold">{targetUserNickname}</div>
            <div className="text-xs flex items-center">
              <i className={`fas fa-circle text-[0.4rem] mr-1 ${isTargetUserOnline ? 'text-green-500' : 'text-gray-500'}`}></i>
              <span className={isTargetUserOnline ? 'text-green-500' : 'text-gray-500'}>
                {isTargetUserOnline ? 'Online' : 'Offline'}
              </span>
            </div>
          </div>
        </div>
      </header>

      {/* Messages */}
      <div ref={scrollContainerRef} className="flex-1 overflow-y-auto p-4 bg-[#f8f9fa]">
        {isLoading && <Loading size="sm" />}
        {renderMessages()}
      </div>

      {/* Input */}
      <form onSubmit={handleSendMessage} className="p-4 bg-white border-t border-[#e9ecef] flex items-center gap-2">
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleImageUpload}
          className="hidden"
        />
        <button
          type="button"
          onClick={handleUploadButtonClick}
          disabled={uploadImageMutation.isPending || !isConnected}
          className="text-accent-color text-xl disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {uploadImageMutation.isPending ? <i className="fas fa-spinner fa-spin"></i> : <i className="fas fa-plus"></i>}
        </button>
        <input
          type="text"
          value={messageInput}
          onChange={(e) => setMessageInput(e.target.value)}
          placeholder="Type a message..."
          className="flex-1 p-3 bg-white border border-[#e9ecef] rounded-full text-text-primary placeholder-gray-400 focus:outline-none focus:border-accent-color"
        />
        <button
          type="submit"
          disabled={!messageInput.trim() || !isConnected}
          className="w-10 h-10 bg-accent-color text-[#121212] rounded-full flex items-center justify-center cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <i className="fas fa-paper-plane"></i>
        </button>
      </form>

      {/* Image Modal */}
      <ImageModal
        isOpen={showImageModal}
        imageSrc={modalImageSrc}
        alt="Chat image"
        onClose={() => setShowImageModal(false)}
      />
    </div>
  );
}

export default ChatRoomPage;
