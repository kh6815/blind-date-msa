import { createContext, useState, useEffect } from 'react';
import { Link, useLocation, Outlet } from 'react-router-dom';
import { useHasUnreadMessages } from '@/features/chat/hooks/useChatRooms';
import { useHasUnreadLikes } from '@/features/home/hooks/useLike';

interface LayoutContextValue {
  onFilterClick?: () => void;
}

export const LayoutContext = createContext<LayoutContextValue>({});

function Layout() {
  const location = useLocation();
  const [onFilterClick, setOnFilterClick] = useState<(() => void) | undefined>();
  const [hasUnreadLikes, setHasUnreadLikes] = useState(false);
  const [hasUnreadChats, setHasUnreadChats] = useState(false);

  // 채팅 미읽음 확인 (React Query Hook 사용)
  const { data: hasUnreadChatsData } = useHasUnreadMessages();

  // 좋아요 미읽음 확인 (React Query Hook 사용)
  const { data: hasUnreadLikesData } = useHasUnreadLikes();

  // Hook에서 받은 데이터를 state에 동기화
  useEffect(() => {
    if (hasUnreadChatsData !== undefined) {
      setHasUnreadChats(hasUnreadChatsData);
    }
  }, [hasUnreadChatsData]);

  useEffect(() => {
    if (hasUnreadLikesData !== undefined) {
      setHasUnreadLikes(hasUnreadLikesData);
    }
  }, [hasUnreadLikesData]);

  // SSE 연결 (좋아요 알림)
  useEffect(() => {
    const likeEventSource = new EventSource(
      `${import.meta.env.VITE_USER_API_URL || '/api/v1/users'}/sse/like-stream`,
      { withCredentials: true }
    );

    likeEventSource.addEventListener('like-notification', () => {
      setHasUnreadLikes(true);
    });

    likeEventSource.onerror = (error) => {
      console.error('Like SSE error:', error);
    };

    return () => {
      likeEventSource.close();
    };
  }, []);

  // SSE 연결 (채팅 알림)
  useEffect(() => {
    const chatEventSource = new EventSource(
      `${import.meta.env.VITE_CHAT_API_URL || '/api/v1/chats'}/sse/badge-stream`,
      { withCredentials: true }
    );

    chatEventSource.addEventListener('badge-update', () => {
      setHasUnreadChats(true);
    });

    chatEventSource.onerror = (error) => {
      console.error('Chat SSE error:', error);
    };

    return () => {
      chatEventSource.close();
    };
  }, []);

  // likes 페이지 방문 시 미읽음 리셋
  useEffect(() => {
    if (location.pathname === '/likes') {
      setHasUnreadLikes(false);
    }
  }, [location.pathname]);

  const getActiveNav = () => {
    if (location.pathname === '/') return 'home';
    if (location.pathname === '/profile') return 'profile';
    if (location.pathname === '/chats') return 'chat';
    if (location.pathname === '/likes') return 'like';
    return '';
  };

  const activeNav = getActiveNav();
  const showFilter = location.pathname === '/';

  return (
    <LayoutContext.Provider value={{ onFilterClick: onFilterClick }}>
      <header className="flex items-center justify-between bg-bg-color p-5 z-10">
        <div className="text-xl font-extrabold tracking-[2px] text-accent-color">
          BLIND DATE
        </div>
        <div>
          {showFilter && onFilterClick && (
            <i
              className="fas fa-sliders-h cursor-pointer text-text-primary"
              onClick={onFilterClick}
            ></i>
          )}
        </div>
      </header>

      <div className="flex-1 overflow-y-auto pb-nav-height w-full">
        <Outlet context={{ setOnFilterClick }} />
      </div>

      <nav className="absolute bottom-0 w-full h-nav-height bg-card-bg flex justify-around items-center border-t border-border-color shadow-[0_-2px_10px_rgba(0,0,0,0.05)]">
        <Link to="/" className={`text-2xl p-2.5 no-underline ${activeNav === 'home' ? 'text-accent-color' : 'text-text-secondary'}`}>
          <i className="fas fa-home"></i>
        </Link>
        <Link to="/chats" className={`text-2xl p-2.5 no-underline ${activeNav === 'chat' ? 'text-accent-color' : 'text-text-secondary'}`}>
          <div className="relative inline-block">
            <i className="fas fa-comment-alt"></i>
            {hasUnreadChats && (
              <span className="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            )}
          </div>
        </Link>
        <Link to="/likes" className={`text-2xl p-2.5 no-underline ${activeNav === 'like' ? 'text-accent-color' : 'text-text-secondary'}`}>
          <div className="relative inline-block">
            <i className="fas fa-heart"></i>
            {hasUnreadLikes && (
              <span className="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            )}
          </div>
        </Link>
        <Link to="/profile" className={`text-2xl p-2.5 no-underline ${activeNav === 'profile' ? 'text-accent-color' : 'text-text-secondary'}`}>
          <i className="fas fa-user"></i>
        </Link>
      </nav>
    </LayoutContext.Provider>
  );
}

export default Layout;