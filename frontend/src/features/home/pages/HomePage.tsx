import { useEffect, useState, useRef } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { useUsers, useUpdateLocation } from '@/features/home/hooks/useUsers';
import type { FilterParams } from '@/types';

function HomePage() {
  const [showFilterModal, setShowFilterModal] = useState(false);
  const [filters, setFilters] = useState<FilterParams>({});
  const sentinelRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { setOnFilterClick } = useOutletContext<{ setOnFilterClick: (fn: () => void) => void }>();
  const updateLocationMutation = useUpdateLocation();

  // useUsers Hook으로 무한 스크롤 처리
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useUsers(filters);

  // 모든 페이지의 사용자를 하나의 배열로 변환
  const users = data?.pages.flatMap((page) => page.data) || [];

  // Register filter click handler
  useEffect(() => {
    setOnFilterClick(() => () => setShowFilterModal(true));
  }, [setOnFilterClick]);

  // Update geolocation on mount
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          updateLocationMutation.mutate({ latitude, longitude });
        },
        (error) => console.warn('Geolocation error:', error)
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Intersection Observer로 무한 스크롤
  useEffect(() => {
    if (!hasNextPage) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.1 }
    );

    const currentSentinel = sentinelRef.current;
    if (currentSentinel) {
      observer.observe(currentSentinel);
    }

    return () => {
      if (currentSentinel) {
        observer.unobserve(currentSentinel);
      }
      observer.disconnect();
    };
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const handleCardClick = (userId: number) => {
    navigate(`/users/${userId}`);
  };

  const handleFilterApply = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setShowFilterModal(false);
  };

  return (
    <>
      <div className="pb-20">
        <div className="grid grid-cols-2 gap-4 p-5">
          {users.map((user) => (
            <div
              key={user.id}
              className="relative aspect-[3/4] rounded-2xl overflow-hidden bg-gray-800 shadow-lg cursor-pointer"
              onClick={() => handleCardClick(user.id)}
            >
              <div
                className="w-full h-full bg-cover bg-center"
                style={{
                  backgroundImage: user.profile_image_url
                    ? `url(${user.profile_image_url})`
                    : 'url(https://randomuser.me/api/portraits/lego/1.jpg)',
                }}
              />

              {user.is_online !== undefined && (
                <div
                  className={`absolute top-2.5 left-2.5 text-white px-2 py-1 rounded-xl text-xs flex items-center gap-1 ${
                    user.is_online
                      ? 'bg-[rgba(76,209,55,0.1)] text-[#4cd137]'
                      : 'bg-[rgba(136,136,136,0.1)] text-gray-400'
                  }`}
                >
                  <div
                    className={`w-1.5 h-1.5 rounded-full ${
                      user.is_online ? 'bg-[#4cd137]' : 'bg-gray-400'
                    }`}
                  ></div>
                  <span>{user.is_online ? 'Online' : 'Offline'}</span>
                </div>
              )}

              {user.distance !== undefined && user.distance !== null && (
                <div className="absolute top-2.5 right-2.5 bg-[rgba(0,0,0,0.5)] text-white px-2 py-1 rounded-xl text-xs">
                  <i className="fas fa-map-marker-alt"></i> {user.distance.toFixed(1)}km
                </div>
              )}

              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-[rgba(0,0,0,0.9)] to-transparent p-4 text-white">
                <div className="text-lg font-bold mb-1">
                  {user.nickname}, {user.age}세
                </div>
                {user.job && (
                  <div className="text-sm text-gray-300">
                    <i className="fas fa-briefcase"></i> {user.job}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {isFetchingNextPage && (
          <div className="text-center p-5">
            <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
          </div>
        )}

        {hasNextPage && <div ref={sentinelRef} className="h-5"></div>}
      </div>

      {/* Filter Modal */}
      {showFilterModal && (
        <div
          className="fixed top-0 left-0 w-full h-full bg-[rgba(0,0,0,0.8)] z-[1000] flex justify-center items-end"
          onClick={(e) => e.target === e.currentTarget && setShowFilterModal(false)}
        >
          <div className="bg-card-bg w-full max-w-[480px] rounded-t-[20px] p-5 animate-slideUp">
            <h2 className="text-text-primary mb-5 text-xl font-bold">필터</h2>
            <form onSubmit={handleFilterApply}>
              <div className="mb-5">
                <label className="block mb-2 text-text-secondary text-sm">나이</label>
                <div className="flex gap-2.5">
                  <input
                    type="number"
                    placeholder="최소"
                    value={filters.minAge || ''}
                    onChange={(e) => setFilters({ ...filters, minAge: e.target.value ? Number(e.target.value) : undefined })}
                    className="flex-1 bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
                  />
                  <input
                    type="number"
                    placeholder="최대"
                    value={filters.maxAge || ''}
                    onChange={(e) => setFilters({ ...filters, maxAge: e.target.value ? Number(e.target.value) : undefined })}
                    className="flex-1 bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
                  />
                </div>
              </div>
              <div className="mb-5">
                <label className="block mb-2 text-text-secondary text-sm">성별</label>
                <select
                  value={filters.gender || ''}
                  onChange={(e) => setFilters({ ...filters, gender: e.target.value || undefined })}
                  className="w-full bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
                >
                  <option value="">전체</option>
                  <option value="MALE">남성</option>
                  <option value="FEMALE">여성</option>
                </select>
              </div>
              <div className="mb-5">
                <label className="block mb-2 text-text-secondary text-sm">MBTI</label>
                <input
                  type="text"
                  placeholder="예: INTJ"
                  value={filters.mbti || ''}
                  onChange={(e) => setFilters({ ...filters, mbti: e.target.value || undefined })}
                  className="w-full bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
                />
              </div>
              <div className="mb-5">
                <label className="block mb-2 text-text-secondary text-sm">지역</label>
                <input
                  type="text"
                  placeholder="예: 서울"
                  value={filters.location || ''}
                  onChange={(e) => setFilters({ ...filters, location: e.target.value || undefined })}
                  className="w-full bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
                />
              </div>
              <button
                type="submit"
                className="w-full bg-accent-color text-white p-4 border-none rounded-lg font-bold text-base cursor-pointer hover:bg-accent-hover"
              >
                필터 적용하기
              </button>
            </form>
          </div>
        </div>
      )}
    </>
  );
}

export default HomePage;
