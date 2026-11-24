import { useState, useEffect } from 'react';
import type { FilterParams } from '@/types';

interface FilterModalProps {
  isOpen: boolean;
  initialFilters: FilterParams;
  onClose: () => void;
  onApply: (filters: FilterParams) => void;
}

function FilterModal({ isOpen, initialFilters, onClose, onApply }: FilterModalProps) {
  const [tempFilters, setTempFilters] = useState<FilterParams>(initialFilters);

  // 모달이 열릴 때마다 현재 필터 값으로 동기화
  useEffect(() => {
    if (isOpen) {
      setTempFilters(initialFilters);
    }
  }, [isOpen, initialFilters]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onApply(tempFilters);
    onClose();
  };

  return (
    <div
      className="fixed top-0 left-0 w-full h-full bg-[rgba(0,0,0,0.8)] z-[1000] flex justify-center items-end"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-card-bg w-full max-w-[480px] rounded-t-[20px] p-5 animate-slideUp">
        <h2 className="text-text-primary mb-5 text-xl font-bold">필터</h2>
        <form onSubmit={handleSubmit}>
          <div className="mb-5">
            <label className="block mb-2 text-text-secondary text-sm">나이</label>
            <div className="flex gap-2.5">
              <input
                type="number"
                placeholder="최소"
                value={tempFilters.minAge || ''}
                onChange={(e) => setTempFilters({ ...tempFilters, minAge: e.target.value ? Number(e.target.value) : undefined })}
                className="flex-1 bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
              />
              <input
                type="number"
                placeholder="최대"
                value={tempFilters.maxAge || ''}
                onChange={(e) => setTempFilters({ ...tempFilters, maxAge: e.target.value ? Number(e.target.value) : undefined })}
                className="flex-1 bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
              />
            </div>
          </div>
          <div className="mb-5">
            <label className="block mb-2 text-text-secondary text-sm">성별</label>
            <select
              value={tempFilters.gender || ''}
              onChange={(e) => setTempFilters({ ...tempFilters, gender: e.target.value || undefined })}
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
              value={tempFilters.mbti || ''}
              onChange={(e) => setTempFilters({ ...tempFilters, mbti: e.target.value || undefined })}
              className="w-full bg-gray-200 border-none p-2.5 rounded-lg text-text-primary"
            />
          </div>
          <div className="mb-5">
            <label className="block mb-2 text-text-secondary text-sm">지역</label>
            <input
              type="text"
              placeholder="예: 서울"
              value={tempFilters.location || ''}
              onChange={(e) => setTempFilters({ ...tempFilters, location: e.target.value || undefined })}
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
  );
}

export default FilterModal;