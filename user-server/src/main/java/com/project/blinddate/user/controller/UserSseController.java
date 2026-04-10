package com.project.blinddate.user.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.user.dto.UserIdRequest;
import com.project.blinddate.user.service.UserSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Tag(name = "User SSE API", description = "유저 좋아요 알림 SSE")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConst.USER_SSE_API_PREFIX)
public class UserSseController {

    private final UserSseService userSseService;
    private static final Long RECONNECT_TIME_MILLIS = 3000L;

    @Operation(summary = "좋아요 알림 SSE 스트림 구독")
    @GetMapping(value = "/like-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeLikeNotifications(UserIdRequest userIdRequest) throws IOException {
        Long userId = userIdRequest.getId();
        log.info("Like SSE connection attempt: userId={}", userId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.info("Like SSE completed: userId={}", userId);
            userSseService.delete(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("Like SSE timeout: userId={}", userId);
            userSseService.delete(userId, emitter);
        });
        emitter.onError(e -> {
            log.error("Like SSE error: userId={}", userId, e);
            userSseService.delete(userId, emitter);
        });

        userSseService.save(userId, emitter);

        emitter.send(SseEmitter.event()
                .name("connect")
                .data("connected!")
                .reconnectTime(RECONNECT_TIME_MILLIS));

        log.info("Like SSE connected: userId={}", userId);
        return emitter;
    }
}
