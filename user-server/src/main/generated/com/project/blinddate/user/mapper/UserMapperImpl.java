package com.project.blinddate.user.mapper;

import com.project.blinddate.user.domain.User;
import com.project.blinddate.user.dto.UserRegisterRequest;
import com.project.blinddate.user.dto.UserResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-11T13:47:13+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.13 (Amazon.com Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRegisterRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.passwordHash( request.getPassword() );
        user.email( request.getEmail() );
        user.nickname( request.getNickname() );
        user.gender( request.getGender() );
        user.birthDate( request.getBirthDate() );
        user.mbti( request.getMbti() );
        user.interests( request.getInterests() );
        user.job( request.getJob() );
        user.description( request.getDescription() );
        user.location( request.getLocation() );
        user.latitude( request.getLatitude() );
        user.longitude( request.getLongitude() );

        return user.build();
    }

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( user.getId() );
        userResponse.email( user.getEmail() );
        userResponse.nickname( user.getNickname() );
        userResponse.gender( user.getGender() );
        userResponse.birthDate( user.getBirthDate() );
        userResponse.mbti( user.getMbti() );
        userResponse.interests( user.getInterests() );
        userResponse.profileImageUrl( user.getProfileImageUrl() );
        userResponse.job( user.getJob() );
        userResponse.description( user.getDescription() );
        userResponse.location( user.getLocation() );
        userResponse.latitude( user.getLatitude() );
        userResponse.longitude( user.getLongitude() );

        return userResponse.build();
    }
}
