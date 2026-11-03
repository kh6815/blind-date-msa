import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/useAuthStore';
import { useInfiniteChatMessages, useMarkMessagesAsRead } from '@/features/chat/hooks/useChatRooms';
import type { ChatMessage, ChatMessageSendRequest, ChatRoom } from '@/types/chat';

function ChatRoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const markMessagesAsReadMutation = useMarkMessagesAsRead();

  const [realtimeMessages, setRealtimeMessages] = useState<ChatMessage[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [isFirstLoad, setIsFirstLoad] = useState(true);

  const roomInfo = location.state?.room as ChatRoom | null;

  const stompClient = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesAreaRef = useRef<HTMLDivElement>(null);

  // 무한 스크롤 Hook으로 메시지 로드
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteChatMessages(roomId || '', 50);

  // 모든 페이지의 메시지를 하나의 배열로 변환
  const messages = data?.pages.flatMap((page) => page) || [];

  // 전체 메시지 = 서버에서 로드한 메시지 + 실시간 메시지
  const allMessages = [...messages, ...realtimeMessages];

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 첫 로드 시 스크롤을 맨 아래로
  useEffect(() => {
    if (isFirstLoad && messages.length > 0) {
      scrollToBottom();
      setIsFirstLoad(false);
    }
  }, [messages.length, isFirstLoad]);

  // 무한 스크롤 - 이전 메시지 로드
  const loadMoreMessages = () => {
    if (!isFetchingNextPage && hasNextPage) {
      const previousScrollHeight = messagesAreaRef.current?.scrollHeight || 0;

      fetchNextPage().then(() => {
        // 스크롤 위치 유지
        setTimeout(() => {
          if (messagesAreaRef.current) {
            const newScrollHeight = messagesAreaRef.current.scrollHeight;
            messagesAreaRef.current.scrollTop = newScrollHeight - previousScrollHeight;
          }
        }, 0);
      });
    }
  };

  // 스크롤 이벤트 핸들러
  useEffect(() => {
    const messagesArea = messagesAreaRef.current;
    if (!messagesArea) return;

    const handleScroll = () => {
      if (messagesArea.scrollTop === 0 && !isFetchingNextPage && hasNextPage) {
        loadMoreMessages();
      }
    };

    messagesArea.addEventListener('scroll', handleScroll);
    return () => messagesArea.removeEventListener('scroll', handleScroll);
  }, [isFetchingNextPage, hasNextPage]);

  // WebSocket 연결
  useEffect(() => {
    if (!roomId || !user) return;

    const socket = new SockJS(`${window.location.protocol}//${window.location.host}/ws/chat`);
    const client = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log(str);
      },
      onConnect: () => {
        console.log('WebSocket connected');
        setIsConnected(true);

        // 메시지 구독
        client.subscribe(`/topic/chats/${roomId}`, (message) => {
          const newMessage: ChatMessage = JSON.parse(message.body);
          setRealtimeMessages((prev) => [...prev, newMessage]);

          // 다른 사람이 보낸 메시지는 자동으로 읽음 처리
          if (newMessage.sender_user_id !== user.id) {
            markAllAsRead();
          }
        });

        // 읽음 이벤트 구독
        client.subscribe(`/topic/chats/${roomId}/read`, (message) => {
          const readEvent = JSON.parse(message.body);
          handleReadEvent(readEvent);
        });

        // 채팅방 진입 시 자동 읽음 처리
        markAllAsRead();
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
      onWebSocketClose: () => {
        console.log('WebSocket disconnected');
        setIsConnected(false);
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, [roomId, user]);

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();

    if (!messageInput.trim() || !stompClient.current || !isConnected || !user || !roomId) {
      return;
    }

    const message: ChatMessageSendRequest = {
      sender_user_id: user.id,
      content: messageInput.trim(),
      type: 'TEXT',
    };

    stompClient.current.publish({
      destination: `/pub/chats/${roomId}`,
      body: JSON.stringify(message),
    });

    setMessageInput('');
  };

  // 읽음 처리
  const markAllAsRead = () => {
    if (!roomId || !user) return;
    markMessagesAsReadMutation.mutate(roomId);
  };

  // 읽음 이벤트 처리
  const handleReadEvent = (readEvent: { user_id: number; read_at: string }) => {
    if (!user || readEvent.user_id === user.id) return;

    const readAtTimestamp = new Date(readEvent.read_at);

    setRealtimeMessages((prevMessages) =>
      prevMessages.map((msg) => {
        if (msg.sender_user_id !== user.id) return msg;

        const sentAtTimestamp = new Date(msg.sent_at);
        if (sentAtTimestamp <= readAtTimestamp && msg.unread_count && msg.unread_count > 0) {
          return {
            ...msg,
            unread_count: Math.max(0, msg.unread_count - 1),
          };
        }
        return msg;
      })
    );
  };

  // 날짜 구분자를 위한 헬퍼 함수
  const getDateKey = (dateString: string) => {
    return new Date(dateString).toISOString().slice(0, 10);
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
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
              className={`max-w-[250px] px-4 py-2 rounded-2xl ${
                isMyMessage
                  ? 'bg-accent-color text-[#000]'
                  : 'bg-[#e9ecef] text-text-primary'
              }`}
            >
              <div className="break-words">{message.content}</div>
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

  const targetUserNickname = roomInfo?.target_user_nickname || '상대방';
  const targetUserImageUrl = roomInfo?.target_user_image_url;
  const isTargetUserOnline = roomInfo?.is_target_user_online || false;

  return (
    <div className="flex flex-col h-screen bg-[#f8f9fa]">
      {/* Header */}
      <header className="px-4 py-3 flex items-center bg-[#f8f9fa] border-b border-[#e9ecef]">
        <button onClick={() => navigate('/chats')} className="text-text-primary text-xl mr-3">
          <i className="fas fa-chevron-left"></i>
        </button>
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
      </header>

      {/* Messages */}
      <div ref={messagesAreaRef} className="flex-1 overflow-y-auto p-4 bg-[#f8f9fa]">
        {isFetchingNextPage && (
          <div className="text-center py-2">
            <i className="fas fa-spinner fa-spin text-accent-color"></i>
          </div>
        )}
        {renderMessages()}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSendMessage} className="p-4 bg-white border-t border-[#e9ecef] flex items-center gap-2">
        <button type="button" className="text-accent-color text-xl">
          <i className="fas fa-plus"></i>
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
    </div>
  );
}

export default ChatRoomPage;
