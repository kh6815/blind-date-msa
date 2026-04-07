package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.dto.ChatUserIdRequest;
import com.project.blinddate.chat.service.SseService;
import com.project.blinddate.common.ApiPathConst;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPathConst.CHAT_SSE_API_PREFIX)
public class SseController {
    private final SseService sseService;

//    private static final Long TIMEOUT = 30000L; //timeout = 30초
    private static final Long RECONNECT_TIME_MILLIS = 3000L; //retry = 3초

    @Operation(summary = "뱃지 업데이트 SSE 스트림", description = "사용자별 뱃지 업데이트 이벤트를 SSE로 수신합니다.")
    @GetMapping(value = "/badge-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeBadgeUpdates(ChatUserIdRequest chatUserIdRequest) throws IOException {
        Long userId = chatUserIdRequest.getCurrentUserId();

        log.info("SSE 연결 시도");

//        SseEmitter sseEmitter = new SseEmitter(TIMEOUT);
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
//        sseEmitter.onCompletion(() -> sseService.delete(userId, sseEmitter)); //SSE 연결 종료 시
//        sseEmitter.onTimeout(() -> sseService.delete(userId, sseEmitter)); //SSE 타임아웃 발생 시
//        sseEmitter.onError(e -> sseService.delete(userId, sseEmitter)); //SSE 연결 중 에러 발생 시

        sseEmitter.onCompletion(() -> {
            log.info("SSE completed userId={}", userId);
            sseService.delete(userId, sseEmitter);
        });

        sseEmitter.onTimeout(() -> {
            log.info("SSE timeout userId={}", userId);
            sseService.delete(userId, sseEmitter);
        });

        sseEmitter.onError(e -> {
            log.error("SSE error userId={}", userId, e);
            sseService.delete(userId, sseEmitter);
        });

        sseService.save(userId, sseEmitter);

        //TODO: 클라이언트가 수신하지 못한 이벤트 처리

        SseEmitter.SseEventBuilder dummyData = SseEmitter.event()
                .name("connect")
                .data("connected!")
                .reconnectTime(RECONNECT_TIME_MILLIS);
        sseEmitter.send(dummyData); //첫 연결 시 더미 데이터 전송
        log.info("SSE 연결 성공 userId={}, 현재 연결 수={}", userId, sseService.getConnectionCount());
        return sseEmitter;
    }
}
