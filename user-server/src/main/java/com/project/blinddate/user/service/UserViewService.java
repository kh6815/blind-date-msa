package com.project.blinddate.user.service;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserView;
import com.project.blinddate.user.repository.UserRepository;
import com.project.blinddate.user.repository.UserViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserViewService {

    private final UserViewRepository userViewRepository;
    private final UserRepository userRepository;

    /**
     * 프로필 조회 기록 저장 (중복 방지)
     * @param viewerId 조회한 사람 ID
     * @param viewedId 조회당한 사람 ID (프로필 주인)
     */
    @Transactional
    public void recordView(Long viewerId, Long viewedId) {
        // 자기 자신의 프로필 조회는 기록하지 않음
        if (viewerId.equals(viewedId)) {
            return;
        }

        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new IllegalArgumentException("Viewer not found: " + viewerId));
        User viewed = userRepository.findById(viewedId)
                .orElseThrow(() -> new IllegalArgumentException("Viewed user not found: " + viewedId));

        // 이미 조회 기록이 있으면 저장하지 않음 (unique constraint에 의해 중복 방지)
        if (userViewRepository.existsByViewerAndViewedAndDelYnFalse(viewer, viewed)) {
            log.debug("View record already exists: viewer={}, viewed={}", viewerId, viewedId);
            return;
        }

        UserView userView = UserView.builder()
                .viewer(viewer)
                .viewed(viewed)
                .build();

        userViewRepository.save(userView);
        log.info("Recorded profile view: viewer={}, viewed={}", viewerId, viewedId);
    }

    /**
     * 특정 유저의 프로필을 조회한 고유 사용자 수 조회
     * @param userId 프로필 주인 ID
     * @return 조회수
     */
    public long getViewCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return userViewRepository.countByViewedAndDelYnFalse(user);
    }
}