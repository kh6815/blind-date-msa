import { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import { useLogin } from '@/features/auth/hooks/useAuth';
import { handleError } from '@/lib/queryClient';

function LoginPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const { setUser } = useAuthStore();

  const loginMutation = useLogin();

  // 공통 로그인 처리 함수
  const performLogin = (loginEmail: string, loginPassword: string) => {
    loginMutation.mutate(
      { email: loginEmail, password: loginPassword },
      {
        onSuccess: (data) => {
          console.log('Login success:', data);
          setUser(data);
          navigate('/');
        },
        onError: (error) => {
          console.error('Login failed:', error);
        },
      }
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    performLogin(email, password);
  };

  // 쿼리 파라미터에서 email과 password를 읽어 자동 로그인
  useEffect(() => {
    const emailParam = searchParams.get('email');
    const passwordParam = searchParams.get('password');

    if (emailParam && passwordParam) {
        // 쿼리 파라미터로 입력 필드 채우기
        setEmail(emailParam);
        setPassword(passwordParam);

        // 자동 로그인 실행
        performLogin(emailParam, passwordParam);
    }
  }, [searchParams]);

  return (
    <div className="flex flex-col justify-center h-full p-5">
      <div className="text-5xl font-extrabold text-accent-color text-center mb-10 tracking-[5px]">
        BLIND DATE
      </div>

      {loginMutation.isError && (
        <div className="text-[#ff4d4d] bg-[rgba(255,77,77,0.1)] p-2.5 rounded-lg mb-5 text-center text-sm">
          {handleError(loginMutation.error)}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="mb-5">
          <input
            type="email"
            name="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full p-4 bg-white border-2 border-border-color rounded-lg text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
            required
          />
        </div>
        <div className="mb-5">
          <input
            type="password"
            name="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full p-4 bg-white border-2 border-border-color rounded-lg text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
            required
          />
        </div>
        <button
          type="submit"
          disabled={loginMutation.isPending}
          className="w-full p-4 bg-accent-color border-none rounded-lg text-white text-base font-bold cursor-pointer mt-5 transition-all active:scale-[0.98] hover:bg-accent-hover disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loginMutation.isPending ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <div className="mt-5 text-center">
        <Link to="/signup" className="text-text-secondary no-underline text-sm hover:text-accent-color">
          회원가입하기
        </Link>
      </div>
    </div>
  );
}

export default LoginPage;