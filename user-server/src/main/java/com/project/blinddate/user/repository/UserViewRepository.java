package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserViewRepository extends JpaRepository<UserView, Long> {

    /**
     * 특정 유저의 프로필을 조회한 고유 사용자 수 카운트
     */
    long countByViewedAndDelYnFalse(User viewed);

    /**
     * 이미 조회 기록이 있는지 확인
     */
    boolean existsByViewerAndViewedAndDelYnFalse(User viewer, User viewed);

    /**
     * 특정 조회 기록 조회
     */
    Optional<UserView> findByViewerAndViewedAndDelYnFalse(User viewer, User viewed);
}