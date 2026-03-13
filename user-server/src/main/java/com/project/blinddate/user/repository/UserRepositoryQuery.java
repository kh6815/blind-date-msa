package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.dto.UserSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRepositoryQuery {

    List<User> searchRecommendUsers(String gender, String mbti, List<String> interests, int limit);

    Page<User> searchUsers(UserSearchCondition condition, Pageable pageable);

    Page<User> searchUsersSortedByDistance(UserSearchCondition condition, double lat, double lon, Pageable pageable);
}


