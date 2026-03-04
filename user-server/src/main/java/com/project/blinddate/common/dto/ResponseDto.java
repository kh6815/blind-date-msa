package com.project.blinddate.common.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 DTO.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseDto<T> {

    private final int status;
    private final String message;
    private final T data;

    public static <T> ResponseDto<T> ok(T data) {
        return ResponseDto.<T>builder()
                .status(200)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> of(int status, String message, T data) {
        return ResponseDto.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}


