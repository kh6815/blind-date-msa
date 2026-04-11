package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryQuery {

    // 삭제되지 않은 유저만 조회 (findById 재정의)
    @Override
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.delYn = false")
    Optional<User> findById(@Param("id") Long id);

    Optional<User> findByEmailAndDelYnFalse(String email);

    boolean existsByEmailAndDelYnFalse(String email);
}


