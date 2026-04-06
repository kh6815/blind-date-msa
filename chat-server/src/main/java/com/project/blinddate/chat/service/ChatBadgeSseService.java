package com.project.blinddate.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 뱃지 업데이트 SSE 서비스
 *
 * 각 서버 인스턴스가 자신에게 연결된 SSE emitter만 로컬 메모리에서 관리합니다.
 * Redis Pub/Sub을 통해 모든 서버에 이벤트를 브로드캐스트하면,
 * 연결된 서버만 클라이언트에게 SSE 이벤트를 전송합니다.
 *
 * 다중 서버 환경에서도 Redis Session 없이 정상 작동합니다.
 */
@Slf4j
@Service
public class ChatBadgeSseService {
    // userId → SseEmitter 매핑 (로컬 메모리)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트 구독 (SSE 연결)
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃 없음
        emitters.put(userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("ok"));

        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        // 연결 종료/타임아웃/에러 시 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        log.info("SSE connected for userId: {}", userId);
        return emitter;
    }

    /**
     * 특정 사용자에게 뱃지 업데이트 알림
     */
    public void notifyBadgeUpdate(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("badge-update")
                        .data("true"));
                log.info("Badge update sent to userId: {}", userId);
            } catch (IOException e) {
                emitters.remove(userId);
                log.warn("Failed to send badge update, removed emitter for userId: {}", userId);
            }
        }
    }
}
