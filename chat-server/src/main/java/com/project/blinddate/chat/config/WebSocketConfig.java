package com.project.blinddate.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
/**
 * WebSocket 설정 클래스
 *
 * STOMP 프로토콜 기반의 WebSocket 메시지 브로커를 설정합니다.
 * 클라이언트와 서버 간의 양방향 통신을 지원하며, 채팅 기능을 구현하는 데 핵심적인 역할을 합니다.
 */
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
        // 구독(Subscribe) 경로: /topic/room/{roomId} 등으로 메시지를 받을 때 사용
        registry.enableSimpleBroker("/topic");
        // 발행(Publish) 경로: /pub/chat/message 등으로 메시지를 보낼 때 사용
        registry.setApplicationDestinationPrefixes("/pub");
    }

    /**
     * STOMP 엔드포인트 등록
     *
     * 클라이언트가 WebSocket 연결을 맺기 위한 엔드포인트를 설정합니다.
     * - addEndpoint("/ws/chat"): "/ws/chat" 경로로 WebSocket 연결을 허용합니다.
     * - setAllowedOriginPatterns("*"): 모든 도메인에서의 접속을 허용합니다 (CORS 설정).
     * - withSockJS(): WebSocket을 지원하지 않는 브라우저에서도 SockJS를 통해 통신할 수 있도록 지원합니다.
     *
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 일반 WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*");
        
        // SockJS 지원 엔드포인트 (Fallback)
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}


