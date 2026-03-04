package com.project.blinddate.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 10)
    private String mbti;

    @Column(length = 255)
    private String interests;

    @Column(length = 255)
    private String profileImageUrl;

    @Column(length = 50)
    private String job;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Builder
    private User(
            Long id,
            String email,
            String passwordHash,
            String nickname,
            String gender,
            LocalDate birthDate,
            String mbti,
            String interests,
            String profileImageUrl,
            String job,
            String description,
            String location,
            Double latitude,
            Double longitude
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.mbti = mbti;
        this.interests = interests;
        this.profileImageUrl = profileImageUrl;
        this.job = job;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateProfile(
            String nickname,
            String mbti,
            String interests,
            String job,
            String description,
            String location,
            Double latitude,
            Double longitude,
            String profileImageUrl
    ) {
        if (nickname != null) this.nickname = nickname;
        if (mbti != null) this.mbti = mbti;
        if (interests != null) this.interests = interests;
        if (job != null) this.job = job;
        if (description != null) this.description = description;
        if (location != null) this.location = location;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }
}


