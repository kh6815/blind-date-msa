import { useEffect, useState, useRef } from 'react';
import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom';
import { useUsers, useUpdateLocation } from '@/features/home/hooks/useUsers';
import FilterModal from '@/features/home/components/FilterModal';
import Loading from '@/shared/components/Loading';
import type { FilterParams } from '@/types';

function HomePage() {
  const [showFilterModal, setShowFilterModal] = useState(false);
  const [filters, setFilters] = useState<FilterParams>({});
  const [searchParams, setSearchParams] = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
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

  // 초기 설정 및 복원 로직을 하나의 useEffect로 통합
  useEffect(() => {
    // 1. Filter click handler 등록
    setOnFilterClick(() => () => setShowFilterModal(true));

    // 2. 필터 복원 (쿼리 파라미터 기반, 초기 로드 시)
    const minAgeParam = searchParams.get('minAge');
    const maxAgeParam = searchParams.get('maxAge');
    const genderParam = searchParams.get('gender');
    const mbtiParam = searchParams.get('mbti');
    const locationParam = searchParams.get('location');

    if (minAgeParam || maxAgeParam || genderParam || mbtiParam || locationParam) {
      setFilters({
          minAge: minAgeParam ? parseInt(minAgeParam, 10) : undefined,
          maxAge: maxAgeParam ? parseInt(maxAgeParam, 10) : undefined,
          gender: genderParam || undefined,
          mbti: mbtiParam || undefined,
          location: locationParam || undefined,
      });
    }

    // 3. Geolocation 업데이트 (초기 로드 시에만)
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          updateLocationMutation.mutate({ latitude, longitude });
        },
        (error) => console.warn('Geolocation error:', error)
      );
    }
  }, []);

  // 페이지 및 스크롤 위치 복원
  useEffect(() => {
    const scrollParam = searchParams.get('scroll');
    const pageParam = searchParams.get('page');

    if (scrollParam && pageParam) {
        setIsLoading(true); // 로딩 시작

      const targetScroll = parseInt(scrollParam, 10);
      const targetPage = parseInt(pageParam, 10);
      const loadedPages = data?.pages.length || 0;

      if (loadedPages <= targetPage && !isFetchingNextPage) {
        fetchNextPage();
      } else if (loadedPages > targetPage && scrollContainerRef.current) {
        requestAnimationFrame(() => {
          scrollContainerRef.current!.scrollTop = targetScroll;
          setIsLoading(false);
        });

        // 스크롤 복원 완료 후 쿼리 파라미터 제거
        const newParams = new URLSearchParams(searchParams);
        newParams.delete('scroll');
        newParams.delete('page');
        setSearchParams(newParams);
      }
    } else {
      setIsLoading(false);
    }
  }, [searchParams, data]);

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
    // scrollContainerRef에서 스크롤 위치 가져오기
    const currentScroll = scrollContainerRef.current?.scrollTop || 0;
    const currentPage = (data?.pages.length || 1) - 1;

    const params = new URLSearchParams({
      returnScroll: currentScroll.toString(),
      returnPage: currentPage.toString(),
    });

    // 필터 정보 추가
    if (filters.minAge !== undefined) params.append('minAge', filters.minAge.toString());
    if (filters.maxAge !== undefined) params.append('maxAge', filters.maxAge.toString());
    if (filters.gender) params.append('gender', filters.gender);
    if (filters.mbti) params.append('mbti', filters.mbti);
    if (filters.location) params.append('location', filters.location);

    navigate(`/users/${userId}?${params.toString()}`);
  };

  const handleFilterApply = (newFilters: FilterParams) => {
    setFilters(newFilters);
  };

  return (
    <>
      <div ref={scrollContainerRef} className="h-full overflow-y-auto pb-20">
        {isLoading && <Loading />}

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

        {isFetchingNextPage && <Loading />}

        {hasNextPage && <div ref={sentinelRef} className="h-5"></div>}
      </div>

      <FilterModal
        isOpen={showFilterModal}
        initialFilters={filters}
        onClose={() => setShowFilterModal(false)}
        onApply={handleFilterApply}
      />
    </>
  );
}

export default HomePage;
