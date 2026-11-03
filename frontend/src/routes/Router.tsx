import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { useCurrentUser } from '@/features/profile/hooks/useCurrentUser';
import Layout from '@/shared/components/Layout';
import LoginPage from '@/features/auth/pages/LoginPage';
import SignupPage from '@/features/auth/pages/SignupPage';
import LogoutPage from '@/features/auth/pages/LogoutPage';
import HomePage from '@/features/home/pages/HomePage';
import UserDetailPage from '@/features/home/pages/UserDetailPage';
import ProfilePage from '@/features/profile/pages/ProfilePage';
import EditProfilePage from '@/features/profile/pages/EditProfilePage';
import LikeListPage from '@/features/likes/pages/LikeListPage';
import ChatListPage from '@/features/chat/pages/ChatListPage';
import ChatRoomCreatePage from '@/features/chat/pages/ChatRoomCreatePage';
import ChatRoomPage from '@/features/chat/pages/ChatRoomPage';

function PrivateRouteWrapper() {
  const { data: user, isLoading, isError } = useCurrentUser();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
      </div>
    );
  }

  if (isError || !user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 로그인 페이지 - 레이아웃 없음 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/logout" element={<LogoutPage />} />

        {/* 메인 레이아웃이 적용되는 페이지들 */}
        <Route element={<PrivateRouteWrapper />}>
          <Route element={<Layout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/users/:userId" element={<UserDetailPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/profile/edit" element={<EditProfilePage />} />
            <Route path="/chats" element={<ChatListPage />} />
            <Route path="/chats/room/create" element={<ChatRoomCreatePage />} />
            <Route path="/likes" element={<LikeListPage />} />
          </Route>
          {/* 채팅방 페이지 - 레이아웃 없음 (전체 화면 사용) */}
          <Route path="/chats/room/:roomId" element={<ChatRoomPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default AppRouter;