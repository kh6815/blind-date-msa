package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    List<UserImage> findByUser(User user);
    void deleteByImageUrlIn(List<String> imageUrls);
}
