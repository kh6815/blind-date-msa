package com.project.blinddate.user.repository;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.dto.UserSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import static com.project.blinddate.user.domain.QUser.user;

@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<User> searchRecommendUsers(String gender, String mbti, List<String> interests, int limit) {
        return queryFactory
                .selectFrom(user)
                .where(
                        genderEq(gender),
                        mbtiEq(mbti),
                        interestsContainsAny(interests)
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<User> searchUsers(UserSearchCondition condition, Pageable pageable) {
        List<User> content = queryFactory
                .selectFrom(user)
                .where(
                        genderEq(condition.getGender()),
                        mbtiEq(condition.getMbti()),
                        locationEq(condition.getLocation()),
                        jobEq(condition.getJob()),
                        ageBetween(condition.getMinAge(), condition.getMaxAge())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        genderEq(condition.getGender()),
                        mbtiEq(condition.getMbti()),
                        locationEq(condition.getLocation()),
                        jobEq(condition.getJob()),
                        ageBetween(condition.getMinAge(), condition.getMaxAge())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression genderEq(String gender) {
        return StringUtils.hasText(gender) ? user.gender.eq(gender) : null;
    }

    private BooleanExpression mbtiEq(String mbti) {
        return StringUtils.hasText(mbti) ? user.mbti.eq(mbti) : null;
    }

    private BooleanExpression locationEq(String location) {
        return StringUtils.hasText(location) ? user.location.contains(location) : null;
    }

    private BooleanExpression jobEq(String job) {
        return StringUtils.hasText(job) ? user.job.contains(job) : null;
    }

    private BooleanExpression ageBetween(Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) {
            return null;
        }
        LocalDate now = LocalDate.now();
        BooleanExpression predicate = null;

        if (minAge != null) {
            // age >= minAge -> birthDate <= now - minAge
            predicate = user.birthDate.loe(now.minusYears(minAge));
        }
        if (maxAge != null) {
            // age <= maxAge -> birthDate >= now - maxAge - 1 (roughly, simplified)
            // e.g. age 20 -> born <= 2004 (if now 2024).
            // Actually, age calculation is floor(years).
            // So if born 2004-01-01, today 2024-01-01, age is 20.
            // If born 2003-12-31, age is 20.
            // to be <= maxAge (e.g. 20), must be born AFTER (now - (maxAge + 1) years).
            // e.g. maxAge 20. Must be born after 2003 (roughly).
            BooleanExpression maxCondition = user.birthDate.gt(now.minusYears(maxAge + 1));
            predicate = predicate == null ? maxCondition : predicate.and(maxCondition);
        }
        return predicate;
    }

    private BooleanExpression interestsContainsAny(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return null;
        }
        BooleanExpression predicate = null;
        for (String interest : interests) {
            BooleanExpression contains = user.interests.containsIgnoreCase(interest);
            predicate = predicate == null ? contains : predicate.or(contains);
        }
        return predicate;
    }
}


