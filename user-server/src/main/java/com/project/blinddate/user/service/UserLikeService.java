package com.project.blinddate.user.service;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.domain.UserLike;
import com.project.blinddate.user.dto.LikeNotificationResponse;
import com.project.blinddate.user.repository.UserLikeRepository;
import com.project.blinddate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLikeService {

    private final UserLikeRepository userLikeRepository;
    private final UserRepository userRepository;
    private final UserLikeRedisPublisher userLikeRedisPublisher;

    @Transactional
    public void like(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신에게 좋아요를 누를 수 없습니다.");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (userLikeRepository.existsByActorAndTargetAndDelYnFalse(actor, target)) {
            throw new IllegalStateException("이미 좋아요를 눌렀습니다.");
        }

        // Soft delete된 좋아요가 있는지 확인하고, 있으면 복원
        userLikeRepository.findByActorAndTargetAndDelYnTrue(actor, target)
                .ifPresentOrElse(
                        existingLike -> {
                            existingLike.restore();
                            existingLike.resetReadStatus();
                            log.info("Like restored: actorId={} -> targetId={}", actorId, targetId);
                        },
                        () -> {
                            userLikeRepository.save(UserLike.builder()
                                    .actor(actor)
                                    .target(target)
                                    .build());
                            log.info("Like saved: actorId={} -> targetId={}", actorId, targetId);
                        }
                );

        // Redis Pub/Sub으로 모든 user-server 인스턴스에 좋아요 이벤트 전파
        userLikeRedisPublisher.publishLikeNotification(
                targetId, actorId,
                actor.getNickname(), actor.getProfileImageUrl()
        );
    }

    @Transactional
    public void unlike(Long actorId, Long targetId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        UserLike userLike = userLikeRepository.findByActorAndTargetAndDelYnFalse(actor, target)
                .orElseThrow(() -> new IllegalStateException("좋아요 기록이 없습니다."));

        userLike.softDelete();
        log.info("Like removed: actorId={} -> targetId={}", actorId, targetId);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long actorId, Long targetId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        return userLikeRepository.existsByActorAndTargetAndDelYnFalse(actor, target);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long targetId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        return userLikeRepository.countByTargetAndDelYnFalse(target);
    }

    @Transactional(readOnly = true)
    public boolean hasUnreadLikes(Long targetId) {
        return userLikeRepository.existsUnreadByTargetId(targetId);
    }

    @Transactional
    public void markLikesAsRead(Long targetId) {
        userLikeRepository.markAsReadBeforeTime(targetId, LocalDateTime.now());
        log.info("Likes marked as read: targetId={}", targetId);
    }

    @Transactional(readOnly = true)
    public Page<LikeNotificationResponse> getLikers(Long targetId, Pageable pageable) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        return userLikeRepository.findByTargetAndDelYnFalseOrderByUpdatedAtDesc(target, pageable)
                .map(like -> LikeNotificationResponse.builder()
                        .userId(like.getActor().getId())
                        .nickname(like.getActor().getNickname())
                        .profileImageUrl(like.getActor().getProfileImageUrl())
                        .likedAt(like.getUpdatedAt())
                        .isRead(like.isRead())
                        .build());
    }
}
