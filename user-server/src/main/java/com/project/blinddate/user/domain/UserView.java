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
        name = "user_views",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_views_viewer_viewed", columnNames = {"viewer_user_id", "viewed_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_views_viewed_user_id", columnList = "viewed_user_id"),
                @Index(name = "idx_user_views_viewer_user_id", columnList = "viewer_user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 조회한 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_user_id", nullable = false)
    private User viewer;

    /** 조회당한 사람 (프로필 주인) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewed_user_id", nullable = false)
    private User viewed;

    @Builder
    public UserView(User viewer, User viewed) {
        this.viewer = viewer;
        this.viewed = viewed;
    }
}