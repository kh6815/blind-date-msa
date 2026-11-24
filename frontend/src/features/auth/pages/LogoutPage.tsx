import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import Loading from '@/shared/components/Loading';

function LogoutPage() {
  const navigate = useNavigate();
  const { logout } = useAuthStore();

  useEffect(() => {
    // Clear auth state
    logout();

    // Redirect to login
    navigate('/login', { replace: true });
  }, [navigate, logout]);

  return <Loading fullScreen />;
}

export default LogoutPage;
