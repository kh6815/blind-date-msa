import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';

function LogoutPage() {
  const navigate = useNavigate();
  const { logout } = useAuthStore();

  useEffect(() => {
    // Clear auth state
    logout();

    // Redirect to login
    navigate('/login', { replace: true });
  }, [navigate, logout]);

  return (
    <div className="flex justify-center items-center h-full">
      <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
    </div>
  );
}

export default LogoutPage;
