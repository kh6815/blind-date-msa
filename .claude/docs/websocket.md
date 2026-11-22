# WebSocket 실시간 통신 구조

## 목차
1. [WebSocket 개념](#websocket-개념)
2. [STOMP 프로토콜](#stomp-프로토콜)
3. [백엔드 WebSocket 구현](#백엔드-websocket-구현)
4. [프론트엔드 WebSocket 구현](#프론트엔드-websocket-구현)
5. [Redis Pub/Sub 통합](#redis-pubsub-통합)
6. [WebSocket 구독 최적화](#websocket-구독-최적화)
7. [읽음 처리 메커니즘](#읽음-처리-메커니즘)
8. [재연결 및 메시지 동기화](#재연결-및-메시지-동기화)

---

## WebSocket 개념

### WebSocket이란?

WebSocket은 클라이언트와 서버 간 **양방향 실시간 통신**을 제공하는 프로토콜입니다.

#### HTTP vs WebSocket

| 특징 | HTTP | WebSocket |
|------|------|-----------|
| **통신 방식** | 단방향 (요청-응답) | 양방향 (Full-Duplex) |
| **연결** | 요청마다 새 연결 | 한 번 연결, 지속적 통신 |
| **오버헤드** | 높음 (매 요청마다 헤더) | 낮음 (최초 1회 핸드셰이크) |
| **실시간성** | 폴링 필요 (비효율적) | 실시간 푸시 (효율적) |
| **사용 사례** | REST API, 웹 페이지 로딩 | 채팅, 알림, 실시간 협업 |

#### WebSocket 연결 과정

```
1. 클라이언트 → 서버: HTTP Upgrade 요청
   GET /ws/chat HTTP/1.1
   Upgrade: websocket
   Connection: Upgrade

2. 서버 → 클라이언트: 101 Switching Protocols
   HTTP/1.1 101 Switching Protocols
   Upgrade: websocket
   Connection: Upgrade

3. WebSocket 연결 확립 (ws:// 또는 wss://)
4. 양방향 메시지 송수신
5. 연결 종료
```

### 왜 WebSocket을 선택했는가?

**채팅 시스템 요구사항:**
- 메시지 즉시 전달 (실시간성)
- 서버 → 클라이언트 푸시 필요 (읽음 표시, 새 메시지 알림)
- 연결 유지 비용 최소화

**대안 비교:**

| 방식 | 장점 | 단점 | 선택 이유 |
|------|------|------|----------|
| **Short Polling** | 구현 간단 | 서버 부하 높음, 실시간성 떨어짐 | ❌ 비효율적 |
| **Long Polling** | 실시간성 개선 | 연결 재수립 오버헤드 | ❌ 복잡함 |
| **SSE** | 서버→클라이언트 푸시 | 단방향, 브라우저 연결 제한 | ❌ 양방향 필요 |
| **WebSocket** | 양방향, 낮은 지연시간, 효율적 | 복잡한 구현 | ✅ 채팅에 최적 |

---

## STOMP 프로토콜

### STOMP란?

STOMP (Simple Text Oriented Messaging Protocol)는 WebSocket 위에서 동작하는 **메시징 프로토콜**입니다.

**순수 WebSocket의 문제:**
- 메시지 형식 미정의 (텍스트? JSON? Binary?)
- 메시지 라우팅 로직 직접 구현 필요
- 구독/발행 패턴 없음

**STOMP가 해결하는 문제:**
- 표준화된 메시지 프레임 (CONNECT, SEND, SUBSCRIBE, MESSAGE, etc.)
- Topic 기반 메시지 라우팅
- 구독/발행 패턴 내장

### STOMP 메시지 프레임 구조

```
COMMAND
header1:value1
header2:value2

Body^@
```

**예시: 채팅 메시지 전송**
```
SEND
destination:/pub/chats/room123
content-type:application/json

{"sender_user_id":1,"content":"Hello","type":"TEXT"}^@
```

### STOMP Destination 패턴

| Destination 접두사 | 용도 | 예시 |
|-------------------|------|------|
| `/pub/...` | 클라이언트 → 서버 메시지 발행 | `/pub/chats/{roomId}` |
| `/topic/...` | 서버 → 클라이언트 브로드캐스트 | `/topic/chats/{roomId}` |
| `/queue/...` | 서버 → 특정 클라이언트 전송 | `/queue/user/{userId}/notifications` |

---

## 백엔드 WebSocket 구현

### 사용 라이브러리

```xml
<!-- chat-server/build.gradle -->
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### WebSocket 설정 (chat-server)

**파일:** `chat-server/src/main/java/com/project/blinddate/chat/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 토픽 접두사
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지 발행 시 사용할 접두사
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback 지원 (WebSocket 미지원 브라우저 대응)
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**주요 설정 설명:**
- `enableSimpleBroker("/topic", "/queue")`: 메모리 기반 메시지 브로커 활성화
- `setApplicationDestinationPrefixes("/pub")`: 클라이언트 메시지 발행 경로
- `addEndpoint("/ws/chat")`: WebSocket 연결 엔드포인트
- `withSockJS()`: WebSocket 미지원 환경에서 HTTP Long Polling으로 fallback

### WebSocket 컨트롤러

**파일:** `chat-server/src/main/java/com/project/blinddate/chat/controller/ChatWebSocketController.java`

```java
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 전송
     * - 클라이언트가 /pub/chats/{roomId}로 메시지 발행
     * - 처리 후 /topic/chats/{roomId}로 브로드캐스트
     */
    @MessageMapping("/chats/{roomId}")
    public void sendMessage(
        @DestinationVariable String roomId,
        ChatMessageRequest request
    ) {
        // 1. 메시지 검증 및 처리
        ChatMessage message = chatService.processMessage(roomId, request);

        // 2. Redis Pub/Sub로 모든 서버 인스턴스에 브로드캐스트
        chatRedisPublisher.publishMessage(roomId, message);

        // 3. Kafka로 비동기 DB 저장
        chatService.publishMessageSaveEvent(message);
    }

    /**
     * 읽음 처리
     * - 클라이언트가 /pub/chats/{roomId}/read로 발행
     * - 해당 채팅방의 모든 메시지를 읽음 처리
     */
    @MessageMapping("/chats/{roomId}/read")
    public void markMessagesAsRead(
        @DestinationVariable String roomId,
        @Payload Map<String, Object> payload
    ) {
        Long userId = ((Number) payload.get("user_id")).longValue();

        // 1. 읽음 처리 (Timestamp 기반)
        chatService.markMessagesAsReadByTimestamp(roomId, userId);

        // 2. 읽음 이벤트 브로드캐스트
        ChatReadEvent readEvent = new ChatReadEvent(userId, LocalDateTime.now());
        messagingTemplate.convertAndSend(
            "/topic/chats/" + roomId + "/read",
            readEvent
        );
    }
}
```

**메시지 흐름:**
1. 클라이언트가 `/pub/chats/{roomId}`로 메시지 전송
2. `@MessageMapping` 메서드 실행
3. Redis Pub/Sub로 모든 서버 인스턴스에 브로드캐스트
4. 각 서버는 자신에게 연결된 클라이언트에게 `/topic/chats/{roomId}`로 메시지 푸시
5. Kafka로 비동기 MongoDB 저장

---

## 프론트엔드 WebSocket 구현

### 사용 라이브러리

```json
{
  "dependencies": {
    "@stomp/stompjs": "^7.5.0",
    "sockjs-client": "^1.6.1"
  }
}
```

**라이브러리 선택 이유:**
- `@stomp/stompjs`: STOMP 프로토콜 구현, 재연결 지원, TypeScript 타입 제공
- `sockjs-client`: WebSocket 미지원 환경 fallback

### 채팅방 WebSocket Hook (useChatRoomSocket)

**파일:** `frontend/src/features/chat/hooks/useChatRoomSocket.ts`

```typescript
import { useEffect, useState, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useChatRoomSocket(
  roomId: string,
  setLocalMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>,
  onReconnect?: () => void
) {
  const [realtimeMessages, setRealtimeMessages] = useState<ChatMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const stompClient = useRef<Client | null>(null);
  const isFirstConnect = useRef(true);

  // 읽음 처리 함수 ref (Visibility API에서 사용)
  const markAllAsReadRef = useRef<(() => void) | null>(null);

  // 메시지 전송 함수
  const sendMessage = useCallback(
    (content: string, type: 'TEXT' | 'IMAGE' = 'TEXT') => {
      if (!stompClient.current || !isConnected) return;

      stompClient.current.publish({
        destination: `/pub/chats/${roomId}`,
        body: JSON.stringify({
          sender_user_id: user.id,
          content: content.trim(),
          type,
        }),
      });
    },
    [roomId, user, isConnected]
  );

  // WebSocket 연결
  useEffect(() => {
    if (!roomId || !user) return;

    // SockJS를 통한 WebSocket 연결 (fallback 지원)
    const socket = new SockJS(
      `${window.location.protocol}//${window.location.host}/ws/chat`
    );

    const client = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000, // 5초 후 자동 재연결
      heartbeatIncoming: 4000, // 서버 → 클라이언트 heartbeat
      heartbeatOutgoing: 4000, // 클라이언트 → 서버 heartbeat
      debug: () => {}, // 프로덕션에서는 로그 비활성화

      onConnect: () => {
        const isReconnect = !isFirstConnect.current;
        setIsConnected(true);

        // 재연결 시 누락된 메시지 동기화
        if (isReconnect && onReconnect) {
          onReconnect();
        }
        isFirstConnect.current = false;

        // 읽음 처리 함수 정의 (최신 user, stompClient 참조)
        markAllAsReadRef.current = () => {
          if (!stompClient.current || document.visibilityState !== 'visible') {
            return;
          }

          stompClient.current.publish({
            destination: `/pub/chats/${roomId}/read`,
            body: JSON.stringify({ user_id: user.id }),
          });
        };

        // 1. 채팅 메시지 구독
        client.subscribe(`/topic/chats/${roomId}`, (message) => {
          const newMessage: ChatMessage = JSON.parse(message.body);
          setRealtimeMessages((prev) => [...prev, newMessage]);

          // 다른 사람 메시지는 자동 읽음 처리
          if (newMessage.sender_user_id !== user.id) {
            markAllAsReadRef.current?.();
          }
        });

        // 2. 읽음 이벤트 구독
        client.subscribe(`/topic/chats/${roomId}/read`, (message) => {
          const readEvent = JSON.parse(message.body);
          handleReadEvent(readEvent);
        });

        // 채팅방 진입 시 자동 읽음 처리
        markAllAsReadRef.current?.();
      },

      onStompError: (frame) => {
        console.error('[STOMP Error]:', frame);
      },

      onWebSocketClose: () => {
        setIsConnected(false);
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
      markAllAsReadRef.current = null;
      isFirstConnect.current = true;
    };
  }, [roomId, user, onReconnect]);

  // Window Visibility API - 탭 활성화 시 읽음 처리
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        markAllAsReadRef.current?.();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  return {
    realtimeMessages,
    isConnected,
    sendMessage,
  };
}
```

**Hook 설계 포인트:**

1. **SockJS Fallback:** WebSocket 미지원 환경 대응
2. **자동 재연결:** `reconnectDelay: 5000`으로 5초 후 재시도
3. **Heartbeat:** 4초 간격 ping/pong으로 연결 유지 감지
4. **읽음 처리 최적화:**
   - `markAllAsReadRef`를 ref로 관리하여 Visibility API에서 안전하게 호출
   - 탭 비활성화 시 읽음 처리 방지 (백그라운드 읽음 방지)
5. **재연결 콜백:** `onReconnect`으로 누락 메시지 동기화

---

## Redis Pub/Sub 통합

### 서버 이중화 문제

**문제 상황:**
```
User A → Server 1 (WebSocket 연결)
User B → Server 2 (WebSocket 연결)

User A가 메시지 전송 → Server 1에만 도착 → User B는 메시지 받지 못함
```

### Redis Pub/Sub 해결 방안

**아키텍처:**
```
┌─────────────┐           ┌─────────────┐
│  Server 1   │           │  Server 2   │
│  (User A)   │           │  (User B)   │
└──────┬──────┘           └──────┬──────┘
       │                         │
       │  Publish to Redis       │
       │  Channel: chat:{roomId} │
       │                         │
       └─────────┬───────────────┘
                 ▼
         ┌───────────────┐
         │  Redis Pub/Sub│
         │  Channel      │
         └───────┬───────┘
                 │ Subscribe
         ┌───────┴───────┐
         │               │
         ▼               ▼
    Server 1          Server 2
    Push to User A    Push to User B
```

**구현: ChatRedisPublisher**

**파일:** `chat-server/src/main/java/com/project/blinddate/chat/service/ChatRedisPublisher.java`

```java
@Service
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 채팅 메시지를 Redis Pub/Sub로 모든 서버 인스턴스에 브로드캐스트
     * - 채널: chat:message:{roomId}
     * - Subscriber: ChatRedisSubscriber가 모든 서버에서 수신
     */
    public void publishMessage(String roomId, ChatMessage message) {
        String channel = "chat:message:" + roomId;
        redisTemplate.convertAndSend(channel, message);
    }

    /**
     * 채팅방 업데이트 이벤트 발행 (채팅방 목록 갱신용)
     * - O(1) 단일 채널로 모든 채팅방 이벤트 처리
     */
    public void publishChatRoomUpdate(ChatRoomUpdateEvent event) {
        String channel = "chat:room:update";
        redisTemplate.convertAndSend(channel, event);
    }
}
```

**구현: ChatRedisSubscriber**

**파일:** `chat-server/src/main/java/com/project/blinddate/chat/service/ChatRedisSubscriber.java`

```java
@Service
@RequiredArgsConstructor
public class ChatRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis에서 채팅 메시지 수신 → 자신의 WebSocket 클라이언트에게 푸시
     * - 각 서버는 자신에게 연결된 클라이언트에게만 전송
     * - Spring WebSocket Session 관리로 자동 필터링
     */
    @RedisListener(topics = "chat:message:*")
    public void onMessageReceived(ChatMessage message, String channel) {
        // 채널 이름에서 roomId 추출: chat:message:{roomId}
        String roomId = channel.substring("chat:message:".length());

        // /topic/chats/{roomId}로 메시지 푸시
        messagingTemplate.convertAndSend(
            "/topic/chats/" + roomId,
            message
        );
    }

    /**
     * 채팅방 업데이트 이벤트 수신
     * - 사용자별 토픽으로 푸시: /topic/users/{userId}/chat-updates
     */
    @RedisListener(topics = "chat:room:update")
    public void onChatRoomUpdate(ChatRoomUpdateEvent event) {
        // 참여자 각각에게 이벤트 전송
        for (Long userId : event.getParticipantIds()) {
            messagingTemplate.convertAndSend(
                "/topic/users/" + userId + "/chat-updates",
                event
            );
        }
    }
}
```

**Redis 설정:**

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisContainer(
        RedisConnectionFactory connectionFactory,
        ChatRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 채팅 메시지 채널 구독 (패턴 매칭)
        container.addMessageListener(
            subscriber,
            new PatternTopic("chat:message:*")
        );

        // 채팅방 업데이트 채널 구독
        container.addMessageListener(
            subscriber,
            new ChannelTopic("chat:room:update")
        );

        return container;
    }
}
```

**성과:**
- 서버 2대 이중화 환경에서 모든 클라이언트가 메시지 정상 수신
- 서버 다운 시 자동 재연결로 무중단 서비스

---

## WebSocket 구독 최적화

### 최적화 전: N개 채팅방 개별 구독 (O(N))

**문제:**
- 사용자가 100개 채팅방 참여 시 → 100개 WebSocket 구독 생성
- 메모리 사용량 증가, 연결 복잡도 증가

```typescript
// 비효율적인 방식
chatRooms.forEach(room => {
  stompClient.subscribe(`/topic/chats/${room.id}`, callback);
});
```

### 최적화 후: 사용자별 단일 토픽 구독 (O(1))

**개선 방안:**
- 사용자별 단일 토픽 생성: `/topic/users/{userId}/chat-updates`
- 모든 채팅방 이벤트를 단일 토픽으로 집약
- 채팅방 업데이트 이벤트 타입으로 구분

**ChatRoomUpdateEvent DTO:**

```java
public class ChatRoomUpdateEvent {
    private UpdateType updateType; // CREATED, UPDATED, DELETED
    private String roomId;
    private ChatRoom chatRoom; // CREATED 시에만
    private String lastMessagePreview; // UPDATED 시에만
    private LocalDateTime lastMessageAt;
    private Integer unreadMessageCount;
}
```

**프론트엔드 단일 구독 (useChatListSocket):**

**파일:** `frontend/src/features/chat/hooks/useChatListSocket.ts`

```typescript
export function useChatListSocket(
  setRooms: React.Dispatch<React.SetStateAction<ChatRoom[]>>
) {
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    if (!user) return;

    const socket = new SockJS(`${window.location.protocol}//${window.location.host}/ws/chat`);
    const client = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        // 단 1개 토픽 구독으로 모든 채팅방 이벤트 수신
        client.subscribe(
          `/topic/users/${user.id}/chat-updates`,
          (message) => {
            const event: ChatRoomUpdateEvent = JSON.parse(message.body);

            switch (event.update_type) {
              case 'CREATED':
                // 새 채팅방 추가
                setRooms(prev => [event.chat_room!, ...prev]);
                break;

              case 'UPDATED':
                // 마지막 메시지 정보 갱신
                setRooms(prev => prev.map(room =>
                  room.id === event.room_id
                    ? {
                        ...room,
                        last_message_preview: event.last_message_preview,
                        last_message_at: event.last_message_at,
                        unread_message_count: (room.unread_message_count ?? 0) + 1,
                      }
                    : room
                ).sort((a, b) =>
                  new Date(b.last_message_at).getTime() -
                  new Date(a.last_message_at).getTime()
                ));
                break;

              case 'DELETED':
                // 채팅방 삭제
                setRooms(prev => prev.filter(r => r.id !== event.room_id));
                break;
            }
          }
        );
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [user, setRooms]);
}
```

**성과:**
- 채팅방 100개 → WebSocket 구독 1개 (O(N) → O(1))
- 메모리 사용량 감소, 연결 복잡도 단순화

---

## 읽음 처리 메커니즘

### Timestamp 기반 읽음 처리

**기존 방식 (Message ID 기반):**
- 문제: 메시지마다 개별 읽음 처리 → DB 부하
- 복잡도: O(N) (N = 메시지 수)

**개선 방식 (Timestamp 기반):**
- 읽음 시각 이후 모든 메시지 일괄 처리
- 복잡도: O(1)

**백엔드 읽음 처리 (ChatService):**

```java
public void markMessagesAsReadByTimestamp(String roomId, Long userId) {
    LocalDateTime readAt = LocalDateTime.now();

    // MongoDB: sent_at <= readAt인 모든 메시지의 unread_count 감소
    chatMessageRepository.decrementUnreadCountByTimestamp(roomId, userId, readAt);

    // Redis Pub/Sub로 읽음 이벤트 브로드캐스트
    ChatReadEvent readEvent = new ChatReadEvent(userId, readAt);
    chatRedisPublisher.publishReadEvent(roomId, readEvent);
}
```

**MongoDB 쿼리 최적화:**

```java
@Repository
public class ChatMessageRepositoryCustomImpl {

    public void decrementUnreadCountByTimestamp(String roomId, Long userId, LocalDateTime readAt) {
        Query query = new Query(
            Criteria.where("roomId").is(roomId)
                .and("senderId").ne(userId) // 내가 보낸 메시지 제외
                .and("sentAt").lte(readAt)
                .and("unreadCount").gt(0)
        );

        Update update = new Update().inc("unreadCount", -1);

        mongoTemplate.updateMulti(query, update, ChatMessage.class);
    }
}
```

**프론트엔드 읽음 이벤트 처리:**

```typescript
const handleReadEvent = useCallback((readEvent: { user_id: number; read_at: string }) => {
  if (readEvent.user_id === user.id) return; // 내가 읽은 경우 무시

  const readAtTimestamp = new Date(readEvent.read_at);

  // 실시간 메시지 unread_count 업데이트
  setRealtimeMessages(prevMessages =>
    prevMessages.map(msg => {
      if (msg.sender_user_id !== user.id) return msg;

      const sentAtTimestamp = new Date(msg.sent_at);
      if (sentAtTimestamp <= readAtTimestamp && msg.unread_count > 0) {
        return {
          ...msg,
          unread_count: Math.max(0, msg.unread_count - 1),
        };
      }
      return msg;
    })
  );

  // 서버에서 로드한 메시지도 동일하게 처리
  setLocalMessages(/* 위와 동일한 로직 */);
}, [user, setLocalMessages]);
```

**읽음 처리 트리거:**
1. 채팅방 진입 시
2. 다른 사람 메시지 수신 시
3. 탭 활성화 시 (Visibility API)

---

## 재연결 및 메시지 동기화

### 재연결 시나리오

**발생 원인:**
- 네트워크 불안정
- 서버 재시작
- 클라이언트 슬립 모드

### 누락 메시지 동기화 전략

**useChatRoomSocket에서 재연결 처리:**

```typescript
const isFirstConnect = useRef(true);

onConnect: () => {
  const isReconnect = !isFirstConnect.current;

  if (isReconnect && onReconnect) {
    // 재연결 시 콜백 실행
    onReconnect();
  }

  isFirstConnect.current = false;
}
```

**ChatRoomPage에서 동기화 로직:**

```typescript
function ChatRoomPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const lastSyncTimestamp = useRef<string | null>(null);

  // 재연결 시 누락 메시지 가져오기
  const handleReconnect = useCallback(async () => {
    if (!lastSyncTimestamp.current) return;

    // 마지막 동기화 시각 이후 메시지 조회
    const missedMessages = await chatApi.getMessagesSince(
      roomId,
      lastSyncTimestamp.current
    );

    setMessages(prev => [...prev, ...missedMessages]);
  }, [roomId]);

  const { realtimeMessages, isConnected, sendMessage } = useChatRoomSocket(
    roomId,
    setMessages,
    handleReconnect // 재연결 콜백 전달
  );

  // 메시지 수신 시 동기화 타임스탬프 업데이트
  useEffect(() => {
    if (realtimeMessages.length > 0) {
      const latestMessage = realtimeMessages[realtimeMessages.length - 1];
      lastSyncTimestamp.current = latestMessage.sent_at;
    }
  }, [realtimeMessages]);
}
```

**백엔드 누락 메시지 조회 API:**

```java
@GetMapping("/chats/rooms/{roomId}/messages/since")
public ResponseDto<List<ChatMessageResponse>> getMessagesSince(
    @PathVariable String roomId,
    @RequestParam String since
) {
    LocalDateTime sinceTime = LocalDateTime.parse(since);
    List<ChatMessage> missedMessages = chatService.getMessagesSince(roomId, sinceTime);

    return ResponseDto.success(missedMessages);
}
```

---

## 주요 구현 파일 위치

### 백엔드 (chat-server)
- **WebSocket 설정:** `src/main/java/.../chat/config/WebSocketConfig.java`
- **WebSocket 컨트롤러:** `src/main/java/.../chat/controller/ChatWebSocketController.java`
- **Redis Publisher:** `src/main/java/.../chat/service/ChatRedisPublisher.java`
- **Redis Subscriber:** `src/main/java/.../chat/service/ChatRedisSubscriber.java`
- **채팅 서비스:** `src/main/java/.../chat/service/ChatService.java`

### 프론트엔드 (frontend)
- **채팅방 WebSocket Hook:** `src/features/chat/hooks/useChatRoomSocket.ts`
- **채팅 목록 WebSocket Hook:** `src/features/chat/hooks/useChatListSocket.ts`
- **채팅방 페이지:** `src/features/chat/pages/ChatRoomPage.tsx`
- **채팅 목록 페이지:** `src/features/chat/pages/ChatListPage.tsx`

---

## 운영 시 고려사항

### 1. WebSocket 연결 제한
- 브라우저별 동시 연결 수 제한 (Chrome: 도메인당 6개)
- 불필요한 연결 최소화, 단일 연결 재사용

### 2. Heartbeat 설정
- 너무 짧으면 네트워크 부하 증가
- 너무 길면 연결 끊김 감지 지연
- 권장: 4000ms (4초)

### 3. 메시지 크기 제한
- WebSocket 프레임 크기 제한 설정
- 대용량 파일은 HTTP API + MinIO 사용

### 4. 보안
- HTTPS + WSS (암호화된 WebSocket)
- JWT 토큰 검증
- Origin 검증

### 5. 모니터링
- WebSocket 연결 수
- 메시지 처리 지연 시간
- 재연결 빈도
- Redis Pub/Sub 메시지 큐 크기