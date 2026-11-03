import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useSignup } from '@/features/auth/hooks/useAuth';
import { handleError } from '@/lib/queryClient';

interface SignupFormData {
  email: string;
  password: string;
  nickname: string;
  gender: 'MALE' | 'FEMALE';
  birthDate: string;
  job: string;
  mbti: string;
  interests: string;
  description: string;
  location: string;
}

function SignupPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<SignupFormData>({
    email: '',
    password: '',
    nickname: '',
    gender: 'MALE',
    birthDate: '',
    job: '',
    mbti: '',
    interests: '',
    description: '',
    location: '',
  });

  const signupMutation = useSignup();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    signupMutation.mutate(
      {
        email: formData.email,
        password: formData.password,
        nickname: formData.nickname,
        gender: formData.gender,
        birth_date: formData.birthDate,
        job: formData.job || undefined,
        mbti: formData.mbti || undefined,
        interests: formData.interests || undefined,
        description: formData.description || undefined,
        location: formData.location || undefined,
      },
      {
        onSuccess: () => {
          navigate('/login');
        },
      }
    );
  };

  return (
    <div className="flex flex-col h-full bg-bg-color">
      {/* Header */}
      <header className="p-5 flex justify-between items-center bg-bg-color z-10 relative">
        <button onClick={() => navigate(-1)} className="absolute top-5 left-5 text-text-primary text-2xl">
          <i className="fas fa-arrow-left"></i>
        </button>
        <div className="flex-1 text-2xl font-bold text-center text-accent-color">
          회원가입
        </div>
      </header>

      {/* Content */}
      <div className="flex-1 overflow-y-auto pb-nav-height w-full">
        <div className="p-5 pb-12">
          {signupMutation.isError && (
            <div className="mb-5 p-4 bg-[rgba(255,77,77,0.1)] border border-[#ff4d4d] rounded-lg text-[#ff4d4d]">
              {handleError(signupMutation.error)}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">이메일</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
                required
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">비밀번호</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
                required
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">닉네임</label>
              <input
                type="text"
                name="nickname"
                value={formData.nickname}
                onChange={handleChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
                required
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">성별</label>
              <select
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              >
                <option value="MALE">남성</option>
                <option value="FEMALE">여성</option>
              </select>
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">생년월일</label>
              <input
                type="date"
                name="birthDate"
                value={formData.birthDate}
                onChange={handleChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
                required
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">직업</label>
              <input
                type="text"
                name="job"
                value={formData.job}
                onChange={handleChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">MBTI</label>
              <input
                type="text"
                name="mbti"
                value={formData.mbti}
                onChange={handleChange}
                placeholder="예: INTJ"
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">관심사 (쉼표로 구분)</label>
              <input
                type="text"
                name="interests"
                value={formData.interests}
                onChange={handleChange}
                placeholder="운동, 여행, 영화"
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">자기소개</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows={3}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color resize-none"
              />
            </div>

            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">위치</label>
              <input
                type="text"
                name="location"
                value={formData.location}
                onChange={handleChange}
                placeholder="서울 강남구"
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              />
            </div>

            <button
              type="submit"
              disabled={signupMutation.isPending}
              className="w-full p-[15px] bg-accent-color border-none rounded-[10px] text-white text-base font-bold cursor-pointer mt-5 transition-all hover:bg-accent-hover active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {signupMutation.isPending ? '가입 중...' : '가입하기'}
            </button>
          </form>

          <div className="mt-5 text-center">
            <Link to="/login" className="text-text-secondary no-underline text-sm hover:text-accent-color">
              이미 계정이 있으신가요?
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default SignupPage;
