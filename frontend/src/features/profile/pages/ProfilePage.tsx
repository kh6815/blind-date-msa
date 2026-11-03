import { useAuthStore } from '@/store/useAuthStore';
import { Link } from 'react-router-dom';
import { useProfileStats } from '@/features/profile/hooks/useProfile';

function ProfilePage() {
  const user = useAuthStore((state) => state.user);

  // 프로필 통계 데이터 가져오기
  const { data: statsData } = useProfileStats();

  return (
    <div className="flex flex-col items-center p-5">
      <div
        className="w-[120px] h-[120px] rounded-full bg-gray-700 mb-5 bg-cover relative"
        style={{
          backgroundImage: user?.profile_image_url
            ? `url(${user.profile_image_url})`
            : 'url(https://randomuser.me/api/portraits/lego/1.jpg)'
        }}
      >
        <Link
          to="/profile/edit"
          className="absolute bottom-0 right-0 bg-gray-800 w-[30px] h-[30px] rounded-full flex justify-center items-center border-2 border-bg-color"
        >
          <i className="fas fa-pen text-xs text-white"></i>
        </Link>
      </div>

      <h2 className="text-text-primary m-0">
        <span>{user?.nickname || 'User'}</span>, <span>{user?.age || 0}</span>
      </h2>

      <div className="text-[#4cd137] text-sm mt-1 flex items-center gap-1">
        ● Online
      </div>

      <div className="w-full bg-card-bg rounded-2xl p-5 mt-5 flex justify-around">
        <div className="text-center">
          <div className="text-xl font-bold text-accent-color">{statsData?.completeness || 0}%</div>
          <div className="text-xs text-text-secondary">COMPLETE</div>
        </div>
        <div className="text-center">
          <div className="text-xl font-bold text-accent-color">
            <i className="fas fa-eye"></i> <span>{statsData?.view_count || 0}</span>
          </div>
          <div className="text-xs text-text-secondary">VIEWS</div>
        </div>
        <div className="text-center">
          <div className="text-xl font-bold text-accent-color">
            <i className="fas fa-heart"></i> <span>{statsData?.like_count || 0}</span>
          </div>
          <div className="text-xs text-text-secondary">LIKES</div>
        </div>
      </div>

      <div className="w-full mt-8">
        <div className="px-4 pb-2.5 text-xs text-text-secondary font-bold tracking-wide">
          SETTINGS MENU
        </div>
        <Link
          to="/profile/edit"
          className="p-4 border-b border-gray-800 flex justify-between items-center cursor-pointer no-underline text-text-primary"
        >
          <span>
            <i className="fas fa-user-edit w-5 text-center text-accent-color mr-2.5"></i>
            Edit Profile
          </span>
          <i className="fas fa-chevron-right text-text-secondary text-xs"></i>
        </Link>
        <div className="p-4 border-b border-gray-800 flex justify-between items-center cursor-pointer">
          <span>
            <i className="fas fa-user-cog w-5 text-center text-accent-color mr-2.5"></i>
            Account Settings
          </span>
          <i className="fas fa-chevron-right text-text-secondary text-xs"></i>
        </div>
        <div className="p-4 border-b border-gray-800 flex justify-between items-center cursor-pointer">
          <span>
            <i className="fas fa-lock w-5 text-center text-accent-color mr-2.5"></i>
            Privacy
          </span>
          <i className="fas fa-chevron-right text-text-secondary text-xs"></i>
        </div>
        <div className="p-4 border-b border-gray-800 flex justify-between items-center cursor-pointer">
          <span>
            <i className="fas fa-question-circle w-5 text-center text-accent-color mr-2.5"></i>
            Help & Support
          </span>
          <i className="fas fa-chevron-right text-text-secondary text-xs"></i>
        </div>
        <Link
          to="/logout"
          className="p-4 flex justify-between items-center cursor-pointer no-underline text-[#ff4d4d]"
        >
          <span>
            <i className="fas fa-sign-out-alt w-5 text-center mr-2.5"></i>
            Logout
          </span>
          <i className="fas fa-chevron-right text-xs"></i>
        </Link>
      </div>
    </div>
  );
}

export default ProfilePage;