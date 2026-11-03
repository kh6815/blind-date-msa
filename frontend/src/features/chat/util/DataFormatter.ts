// 날짜 구분자를 위한 헬퍼 함수
export const getDateKey = (dateString: string) => {
    return new Date(dateString).toISOString().slice(0, 10);
};

export const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    });
};

export const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
    });
};

/**
 * 상대적 시간 포맷 (채팅 목록용)
 * - 1분 이내: "방금 전"
 * - 1시간 이내: "N분 전"
 * - 24시간 이내: "N시간 전"
 * - 어제: "어제"
 * - 그 외: "yyyy-MM-dd"
 */
export const formatRelativeTime = (dateString: string | undefined, currentTime: Date = new Date()): string => {
    if (!dateString) return '';

    const date = new Date(dateString);
    const diffMs = currentTime.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    // 1분 이내
    if (diffMinutes < 1) return '방금 전';

    // 1시간 이내
    if (diffMinutes < 60) return `${diffMinutes}분 전`;

    // 24시간 이내
    if (diffHours < 24) return `${diffHours}시간 전`;

    // 어제 (24~48시간)
    if (diffDays === 1) return '어제';

    // 그 외: yyyy-MM-dd
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};