package com.project.blinddate.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchCondition {
    private String gender;
    private String mbti;
    private String location;
    private Integer minAge;
    private Integer maxAge;
    private String job;
}
