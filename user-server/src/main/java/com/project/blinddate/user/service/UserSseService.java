package com.project.blinddate.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class UserSseService {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(Long userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void delete(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    public void sendLikeNotification(Long userId, String payload) {
        List<SseEmitter> list = emitters.getOrDefault(userId, List.of());
        list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("like-notification")
                        .data(payload));
                log.info("Like notification sent to userId={}", userId);
            } catch (IOException e) {
                log.warn("Like notification failed, removing emitter userId={}", userId);
                delete(userId, emitter);
            }
        });
    }

    public boolean isConnected(Long userId) {
        List<SseEmitter> list = emitters.get(userId);
        return list != null && !list.isEmpty();
    }
}
