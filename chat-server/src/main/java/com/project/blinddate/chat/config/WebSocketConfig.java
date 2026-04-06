package com.project.blinddate.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 *
 * STOMP 프로토콜 기반의 WebSocket 메시지 브로커를 설정합니다.
 * 채팅 메시지 실시간 전송을 위한 WebSocket 연결을 지원합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     *
     * 메시지를 중간에서 라우팅하는 브로커를 설정합니다.
     * - enableSimpleBroker: 메모리 기반의 내장 메시지 브로커를 활성화합니다.
     *   "/topic"으로 시작하는 주소로 메시지를 발행하면, 해당 주소를 구독하는 클라이언트에게 메시지를 전달합니다.
     * - setApplicationDestinationPrefixes: 클라이언트가 서버로 메시지를 보낼 때 사용하는 주소의 접두사입니다.
     *   "/pub"으로 시작하는 메시지는 @MessageMapping이 붙은 메서드로 라우팅됩니다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독(Subscribe) 경로: /topic/room/{roomId}, /user/{token}/unread-badge 등으로 메시지를 받을 때 사용
        registry.enableSimpleBroker("/topic", "/user");
        // 발행(Publish) 경로: /pub/chat/message 등으로 메시지를 보낼 때 사용
        registry.setApplicationDestinationPrefixes("/pub");
        // 사용자별 메시지 전송을 위한 prefix 설정
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     *
     * 클라이언트가 WebSocket 연결을 맺기 위한 엔드포인트를 설정합니다.
     * - addEndpoint("/ws/chat"): "/ws/chat" 경로로 WebSocket 연결을 허용합니다.
     * - setAllowedOriginPatterns: 허용할 도메인을 설정합니다 (CORS 설정).
     * - withSockJS(): WebSocket을 지원하지 않는 브라우저에서도 SockJS를 통해 통신할 수 있도록 지원합니다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = {
            "http://*.blind-date.site",
            "http://*.blind-date.com",
            "https://*.blind-date.site",
            "https://*.blind-date.com"
        };

        // 일반 WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOrigins);

        // SockJS 지원 엔드포인트 (Fallback)
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}


