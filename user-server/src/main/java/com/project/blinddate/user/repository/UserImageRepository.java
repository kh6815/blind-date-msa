package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {

    List<UserImage> findByUserAndDelYnFalse(User user);

    @Modifying
    @Query("UPDATE UserImage u SET u.delYn = true, u.deletedAt = CURRENT_TIMESTAMP WHERE u.imageUrl IN :imageUrls AND u.delYn = false")
    void softDeleteByImageUrlIn(@Param("imageUrls") List<String> imageUrls);
}
