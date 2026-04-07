package com.project.blinddate.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * API 경로 상수 모음 (Chat 서버용).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPathConst {

    public static final String API_V1_PREFIX = "/api/v1";
    public static final String CHAT_API_PREFIX = API_V1_PREFIX + "/chats";
    public static final String CHAT_SSE_API_PREFIX = API_V1_PREFIX + "/chats/sse";
}


