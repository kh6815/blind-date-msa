package com.project.blinddate.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * API 경로 상수 모음.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPathConst {

    public static final String API_V1_PREFIX = "/api/v1";

    public static final String USER_API_PREFIX = API_V1_PREFIX + "/users";
}


