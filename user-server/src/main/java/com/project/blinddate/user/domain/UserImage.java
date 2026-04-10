package com.project.blinddate.user.domain;

import com.project.blinddate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "user_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @Builder
    public UserImage(User user, String imageUrl) {
        this.user = user;
        this.imageUrl = imageUrl;
    }
}
