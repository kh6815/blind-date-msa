package com.project.blinddate.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {

    private final Map<Long, List<SseEmitter>> sseEmitterRepository = new ConcurrentHashMap<>();

    public List<SseEmitter> getSseEmitters(Long userId) {
        if (!sseEmitterRepository.containsKey(userId)) {
            sseEmitterRepository.put(userId, new CopyOnWriteArrayList<>());
        }
        return sseEmitterRepository.get(userId);
    }

    public void save(Long userId, SseEmitter sseEmitter) {
        getSseEmitters(userId).add(sseEmitter);
    }

    public void delete(Long userId, SseEmitter sseEmitter) {
        getSseEmitters(userId).remove(sseEmitter);
    }

    public int getConnectionCount() {
        return sseEmitterRepository.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public void sendBadgeUpdateMessage(Long userId) {
        List<SseEmitter> sseEmitters = getSseEmitters(userId);
        sseEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("badge-update")
                        .data("true"));
                log.info("Badge update sent to userId={}", userId);
            } catch (IOException e) {
                log.warn("Badge update 전송 실패, emitter 제거 userId={}", userId);
                delete(userId, emitter);
            }
        });
    }

//    // 30초마다 heartbeat 전송 + 죽은 연결 감지
//    @Scheduled(fixedDelay = 30_000)
//    public void sendHeartbeat() {
//        int total = getConnectionCount();
//        if (total == 0) return;
//
//        log.info("[SSE Heartbeat] 현재 연결 수={}", total);
//
//        sseEmitterRepository.forEach((userId, emitters) ->
//            emitters.forEach(emitter -> {
//                try {
//                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
//                } catch (IOException e) {
//                    log.warn("[SSE Heartbeat] 연결 끊김 감지 userId={}", userId);
//                    delete(userId, emitter);
//                }
//            })
//        );
//    }
}
