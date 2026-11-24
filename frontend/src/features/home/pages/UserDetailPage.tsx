import { useState } from 'react';
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useUser, useRecordView } from '@/features/home/hooks/useUsers';
import { useCheckLike, useAddLike, useRemoveLike } from '@/features/home/hooks/useLike';
import ImageModal from '@/shared/components/ImageModal';
import Loading from '@/shared/components/Loading';

function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [showImageModal, setShowImageModal] = useState(false);

  // React Query hooks 사용
  const { data: user } = useUser(userId || '');
  const { data: likeData } = useCheckLike(userId || '');
  const addLikeMutation = useAddLike();
  const removeLikeMutation = useRemoveLike();

  // 조회수 기록 (userId가 바뀌면 실행, 같으면 캐시 사용)
  useRecordView(userId || '');

  const allImages = user
    ? [
        user.profile_image_url || 'https://randomuser.me/api/portraits/lego/1.jpg',
        ...(user.image_urls || []),
      ]
    : [];

  const handleLikeToggle = () => {
    if (!userId) return;

    if (likeData?.is_liked) {
      removeLikeMutation.mutate(Number(userId));
    } else {
      addLikeMutation.mutate(Number(userId));
    }
  };

  const handlePrevImage = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentImageIndex((prev) => (prev - 1 + allImages.length) % allImages.length);
  };

  const handleNextImage = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentImageIndex((prev) => (prev + 1) % allImages.length);
  };

  const handleBackClick = () => {
    const returnScroll = searchParams.get('returnScroll');
    const returnPage = searchParams.get('returnPage');
    const minAge = searchParams.get('minAge');
    const maxAge = searchParams.get('maxAge');
    const gender = searchParams.get('gender');
    const mbti = searchParams.get('mbti');
    const location = searchParams.get('location');

    if (returnScroll && returnPage) {
      // 홈으로 돌아가면서 스크롤 위치 및 필터 정보 전달
      const params = new URLSearchParams({
        scroll: returnScroll,
        page: returnPage,
      });

      // 필터 정보 추가
      if (minAge) params.append('minAge', minAge);
      if (maxAge) params.append('maxAge', maxAge);
      if (gender) params.append('gender', gender);
      if (mbti) params.append('mbti', mbti);
      if (location) params.append('location', location);

      navigate(`/?${params.toString()}`);
    } else {
      // 쿼리 파라미터가 없으면 그냥 뒤로가기
      navigate(-1);
    }
  };

  if (!user) {
    return <Loading />;
  }

  const interestList = user.interests?.split(',').map((i) => i.trim()).filter(Boolean) || [];

  return (
    <div className="pb-20">
      {/* Header */}
      <header className="px-5 py-3 flex items-center bg-bg-color z-10">
        <button onClick={handleBackClick} className="text-text-primary text-2xl">
          <i className="fas fa-arrow-left"></i>
        </button>
      </header>

      {/* Image Slider */}
      <div className="relative w-full h-[400px] overflow-hidden">
        <img
          src={allImages[currentImageIndex]}
          alt={user.nickname}
          className="w-full h-full object-cover cursor-zoom-in"
          onClick={() => setShowImageModal(true)}
        />

        {allImages.length > 1 && (
          <>
            <button
              type="button"
              onClick={handlePrevImage}
              className="absolute top-1/2 left-2.5 -translate-y-1/2 bg-[rgba(0,0,0,0.3)] text-white border-none text-3xl p-2.5 cursor-pointer z-[5] rounded"
            >
              &#8592;
            </button>
            <button
              type="button"
              onClick={handleNextImage}
              className="absolute top-1/2 right-2.5 -translate-y-1/2 bg-[rgba(0,0,0,0.3)] text-white border-none text-3xl p-2.5 cursor-pointer z-[5] rounded"
            >
              &#8594;
            </button>
          </>
        )}
      </div>

      {/* Profile Info */}
      <div className="p-5">

        <div className="flex justify-between items-center mb-1">
          <h1 className="text-3xl font-extrabold text-text-primary m-0">
            {user.nickname}, {user.age}
          </h1>
          <div
            className={`px-2.5 py-1 rounded-2xl text-xs inline-flex items-center gap-1 ${
              user.is_online
                ? 'bg-[rgba(76,209,55,0.2)] text-[#4cd137]'
                : 'bg-[rgba(136,136,136,0.1)] text-gray-400'
            }`}
          >
            <i className="fas fa-circle text-[0.5rem]"></i>
            <span>{user.is_online ? 'Online' : 'Offline'}</span>
          </div>
        </div>

        {user.location && (
          <p className="text-text-secondary text-sm mb-5 flex items-center gap-1">
            <i className="fas fa-map-marker-alt"></i>
            {user.location}
          </p>
        )}

        {user.job && (
          <div className="mb-5">
            <div className="text-sm font-bold text-accent-color mb-2 uppercase tracking-wide">Job</div>
            <div className="text-base text-text-primary">{user.job}</div>
          </div>
        )}

        {user.description && (
          <div className="mb-5">
            <div className="text-sm font-bold text-accent-color mb-2 uppercase tracking-wide">About</div>
            <div className="text-text-secondary leading-relaxed">{user.description}</div>
          </div>
        )}

        {user.mbti && (
          <div className="mb-5">
            <div className="text-sm font-bold text-accent-color mb-2 uppercase tracking-wide">MBTI</div>
            <div className="flex flex-wrap">
              <span className="bg-gray-200 text-text-primary px-2.5 py-1 rounded-2xl text-xs inline-block mr-1 mb-1">
                {user.mbti}
              </span>
            </div>
          </div>
        )}

        {interestList.length > 0 && (
          <div className="mb-5">
            <div className="text-sm font-bold text-accent-color mb-2 uppercase tracking-wide">Interests</div>
            <div className="flex flex-wrap">
              {interestList.map((interest, index) => (
                <span
                  key={index}
                  className="bg-gray-200 text-text-primary px-2.5 py-1 rounded-2xl text-xs inline-block mr-1 mb-1"
                >
                  {interest}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="flex items-center gap-1.5 mt-3 text-sm text-gray-400">
          <i className="fas fa-heart text-red-600 text-sm"></i>
          <span>{likeData?.like_count || 0}명이 좋아요를 눌렀습니다</span>
        </div>

        {/* Action Buttons */}
        <div className="flex gap-4 mt-8 mb-5">
          <Link to="/" className="flex-1 p-4 rounded-[30px] border-none font-bold text-lg cursor-pointer flex justify-center items-center gap-2.5 no-underline transition-transform active:scale-[0.98] bg-gray-800 text-red-600">
            <i className="fas fa-times"></i> Pass
          </Link>
          <button
            type="button"
            onClick={handleLikeToggle}
            className={`flex-1 p-4 rounded-[30px] border-none font-bold text-lg cursor-pointer flex justify-center items-center gap-2.5 transition-transform active:scale-[0.98] ${
              likeData?.is_liked ? 'bg-red-600 text-white' : 'bg-gray-800 text-white'
            }`}
          >
            <i className="fas fa-heart"></i> {likeData?.is_liked ? 'Liked' : 'Like'}
          </button>
          <Link
            to={`/chats/room/create?targetUserId=${user.id}`}
            className="flex-1 p-4 rounded-[30px] border-none font-bold text-lg cursor-pointer flex justify-center items-center gap-2.5 no-underline transition-transform active:scale-[0.98] bg-accent-color text-gray-900"
          >
            <i className="fas fa-comment"></i> Chat
          </Link>
        </div>
      </div>

      {/* Image Modal */}
      <ImageModal
        isOpen={showImageModal}
        imageSrc={allImages[currentImageIndex]}
        alt={user.nickname}
        onClose={() => setShowImageModal(false)}
      />
    </div>
  );
}

export default UserDetailPage;
