package com.project.blinddate.user.domain;

import com.project.blinddate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_likes_actor_target", columnNames = {"actor_user_id", "target_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_likes_target_user_id", columnList = "target_user_id"),
                @Index(name = "idx_user_likes_actor_user_id", columnList = "actor_user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 좋아요를 누른 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    /** 좋아요를 받은 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User target;

    /** 읽음 여부 (target이 like-list 조회 시 true로 전환) */
    @Column(nullable = false)
    private boolean isRead = false;

    @Builder
    public UserLike(User actor, User target) {
        this.actor = actor;
        this.target = target;
    }

    public void resetReadStatus() {
        this.isRead = false;
    }
}
