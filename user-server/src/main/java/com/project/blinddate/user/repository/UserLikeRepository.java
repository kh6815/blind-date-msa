package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {

    boolean existsByActorAndTargetAndDelYnFalse(User actor, User target);

    Optional<UserLike> findByActorAndTargetAndDelYnFalse(User actor, User target);

    long countByTargetAndDelYnFalse(User target);

    Page<UserLike> findByTargetAndDelYnFalseOrderByCreatedAtDesc(User target, Pageable pageable);

    @Query("SELECT COUNT(ul) > 0 FROM UserLike ul WHERE ul.target.id = :targetId AND ul.isRead = false AND ul.delYn = false")
    boolean existsUnreadByTargetId(@Param("targetId") Long targetId);

    @Modifying
    @Query("UPDATE UserLike ul SET ul.isRead = true WHERE ul.target.id = :targetId AND ul.isRead = false AND ul.createdAt <= :readAt AND ul.delYn = false")
    void markAsReadBeforeTime(@Param("targetId") Long targetId, @Param("readAt") LocalDateTime readAt);
}
