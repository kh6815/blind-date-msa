import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLikedUsers, useMarkLikesAsRead } from '@/features/likes/hooks/useLikes';

function LikeListPage() {
  const navigate = useNavigate();
  const sentinelRef = useRef<HTMLDivElement>(null);
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
  } = useLikedUsers();
  const markAsReadMutation = useMarkLikesAsRead();

  // 모든 페이지의 데이터를 평탄화
  const likers = data?.pages.flatMap((page) => page.data) || [];

  // 읽음 처리
  useEffect(() => {
    markAsReadMutation.mutate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Infinite scroll observer
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

  const handleLikerClick = (userId: number) => {
    navigate(`/users/${userId}`);
  };

  const formatDateTime = (dateString: string) => {
    if (!dateString) return '';

    const date = new Date(dateString);

    // 유효하지 않은 날짜 체크
    if (isNaN(date.getTime())) {
      return '';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  };

  return (
    <div className="pb-20">
      {/* 빈 상태 */}
      {likers.length === 0 && !isLoading && (
        <div className="p-[15px_20px] text-center text-gray-500">
          <i className="fas fa-heart text-5xl text-gray-500 mb-3"></i>
          <p>아직 좋아요를 보낸 사람이 없습니다.</p>
        </div>
      )}

      {/* 좋아요 목록 */}
      <div>
        {likers.map((liker) => (
          <div
            key={liker.user_id}
            onClick={() => handleLikerClick(liker.user_id)}
            className="flex items-center p-[15px_20px] border-b border-[#e9ecef] cursor-pointer hover:bg-gray-50 relative"
          >
            {/* 프로필 이미지 */}
            {liker.profile_image_url ? (
              <div
                className="w-[60px] h-[60px] rounded-full bg-gray-200 mr-[15px] bg-cover bg-center flex-shrink-0"
                style={{
                  backgroundImage: `url(${liker.profile_image_url})`,
                }}
              ></div>
            ) : (
              <div className="w-[60px] h-[60px] rounded-full bg-accent-color mr-[15px] flex items-center justify-center text-white font-bold text-xl flex-shrink-0">
                {liker.nickname ? liker.nickname.charAt(0).toUpperCase() : '?'}
              </div>
            )}

            {/* 정보 영역 */}
            <div className="flex-1 min-w-0">
              <div className="font-bold text-base mb-[5px]">{liker.nickname}</div>
              <div className="text-text-secondary text-sm flex items-center">
                <i className="fas fa-heart text-red-500 text-xs mr-1"></i>
                <span>회원님의 프로필에 좋아요를 눌렀습니다</span>
              </div>
            </div>

            {/* 오른쪽 영역 (날짜 + N뱃지) */}
            <div className="flex flex-col items-end gap-2 ml-2">
              <span className="text-xs text-text-secondary whitespace-nowrap">
                {formatDateTime(liker.liked_at)}
              </span>
              {!liker.is_read && (
                <div className="w-6 h-6 bg-red-500 rounded-full flex items-center justify-center">
                  <span className="text-white text-xs font-bold">N</span>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 로딩 인디케이터 */}
      {isLoading && (
        <div className="text-center p-5">
          <i className="fas fa-spinner fa-spin text-2xl text-accent-color"></i>
        </div>
      )}

      {/* Sentinel - hasNextPage가 true일 때만 렌더링 */}
      {hasNextPage && <div ref={sentinelRef} className="h-5"></div>}
    </div>
  );
}

export default LikeListPage;
