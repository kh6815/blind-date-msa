import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import { useUpdateProfile } from '@/features/profile/hooks/useProfile';

interface ProfileFormData {
  nickname: string;
  job: string;
  mbti: string;
  interests: string;
  description: string;
  location: string;
}

function EditProfilePage() {
  const navigate = useNavigate();
  const { user, setUser } = useAuthStore();
  const updateProfileMutation = useUpdateProfile();
  const [formData, setFormData] = useState<ProfileFormData>({
    nickname: '',
    job: '',
    mbti: '',
    interests: '',
    description: '',
    location: '',
  });

  // 이미지 관련 상태
  const [mainImagePreview, setMainImagePreview] = useState<string>('');
  const [mainImageFile, setMainImageFile] = useState<File | null>(null);
  const [existingImages, setExistingImages] = useState<string[]>([]);
  const [deletedImages, setDeletedImages] = useState<string[]>([]);
  const [newImageFiles, setNewImageFiles] = useState<File[]>([]);
  const [newImagePreviews, setNewImagePreviews] = useState<string[]>([]);

  useEffect(() => {
    if (user) {
      setFormData({
        nickname: user.nickname || '',
        job: user.job || '',
        mbti: user.mbti || '',
        interests: user.interests || '',
        description: user.description || '',
        location: user.location || '',
      });
      setMainImagePreview(user.profile_image_url || 'https://randomuser.me/api/portraits/lego/1.jpg');
      // 사용자의 추가 이미지 로드
      setExistingImages(user.image_urls || []);
    }
  }, [user]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleMainImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setMainImageFile(file);
      const reader = new FileReader();
      reader.onload = (event) => {
        setMainImagePreview(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleNewImagesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    setNewImageFiles(files);

    const previews: string[] = [];
    files.forEach((file) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        previews.push(event.target?.result as string);
        if (previews.length === files.length) {
          setNewImagePreviews(previews);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const handleDeleteExistingImage = (imageUrl: string) => {
    setExistingImages((prev) => prev.filter((img) => img !== imageUrl));
    setDeletedImages((prev) => [...prev, imageUrl]);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const formDataToSend = new FormData();

    // 텍스트 필드
    formDataToSend.append('nickname', formData.nickname);
    if (formData.job) formDataToSend.append('job', formData.job);
    if (formData.mbti) formDataToSend.append('mbti', formData.mbti);
    if (formData.interests) formDataToSend.append('interests', formData.interests);
    if (formData.description) formDataToSend.append('description', formData.description);
    if (formData.location) formDataToSend.append('location', formData.location);

    // 프로필 이미지
    if (mainImageFile) {
      formDataToSend.append('profileImageFile', mainImageFile);
    }

    // 추가 이미지
    newImageFiles.forEach((file) => {
      formDataToSend.append('profileImageFiles', file);
    });

    // 삭제할 이미지
    deletedImages.forEach((url) => {
      formDataToSend.append('deletedImages', url);
    });

    // Use mutation with onSuccess callback
    updateProfileMutation.mutate(formDataToSend, {
      onSuccess: (updatedUser) => {
        setUser(updatedUser);
        navigate('/profile');
      },
    });
  };

  return (
    <div className="flex flex-col h-full bg-bg-color">
      {/* Header */}
      <header className="p-5 flex justify-between items-center bg-bg-color z-10 relative">
        <button onClick={() => navigate(-1)} className="absolute top-5 left-5 text-text-primary text-2xl">
          <i className="fas fa-arrow-left"></i>
        </button>
        <div className="flex-1 text-2xl font-bold text-center text-accent-color">
          프로필 수정
        </div>
      </header>

      {/* Content */}
      <div className="flex-1 overflow-y-auto pb-nav-height w-full">
        <div className="p-5 pb-12">
          <form onSubmit={handleSubmit}>
            {/* 프로필 사진 (대표) */}
            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">프로필 사진 (대표)</label>
              <div className="mb-2.5">
                <img
                  src={mainImagePreview}
                  alt="Profile preview"
                  className="w-[100px] h-[100px] rounded-full object-cover border-2 border-accent-color"
                />
              </div>
              <input
                type="file"
                accept="image/*"
                onChange={handleMainImageChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              />
            </div>

            {/* 추가 이미지 (최대 5장) */}
            <div className="mb-5">
              <label className="block mb-2 text-text-secondary">추가 이미지 (최대 5장)</label>

              {/* 기존 이미지 */}
              {existingImages.length > 0 && (
                <div className="flex gap-2.5 mb-2.5 flex-wrap">
                  {existingImages.map((img, index) => (
                    <div key={index} className="relative inline-block">
                      <img
                        src={img}
                        alt={`Existing ${index}`}
                        className="w-[80px] h-[80px] object-cover rounded-[10px] border border-[#444]"
                      />
                      <button
                        type="button"
                        onClick={() => handleDeleteExistingImage(img)}
                        className="absolute -top-[5px] -right-[5px] bg-red-600 text-white border-none rounded-full w-5 h-5 text-xs cursor-pointer flex items-center justify-center"
                      >
                        <i className="fas fa-times"></i>
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <input
                type="file"
                accept="image/*"
                multiple
                onChange={handleNewImagesChange}
                className="w-full p-[15px] bg-white border-2 border-[#e9ecef] rounded-[10px] text-text-primary text-base box-border transition-colors focus:outline-none focus:border-accent-color"
              />

              {/* 새 이미지 미리보기 */}
              {newImagePreviews.length > 0 && (
                <div className="flex gap-2.5 mt-2.5">
                  {newImagePreviews.map((preview, index) => (
                    <img
                      key={index}
                      src={preview}
                      alt={`New preview ${index}`}
                      className="w-[80px] h-[80px] object-cover rounded-[10px]"
                    />
                  ))}
                </div>
              )}
            </div>

            {/* 닉네임 */}
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

            {/* 직업 */}
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

            {/* MBTI */}
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

            {/* 관심사 */}
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

            {/* 자기소개 */}
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

            {/* 위치 */}
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
              className="w-full p-[15px] bg-accent-color border-none rounded-[10px] text-white text-base font-bold cursor-pointer mt-5 transition-all hover:bg-accent-hover active:scale-[0.98]"
            >
              저장하기
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

export default EditProfilePage;
