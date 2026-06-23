# chat-server 모듈 구조

## 목적

chat-server의 실시간 메시지 처리 구조(WebSocket + Redis Pub/Sub + Kafka 3단 구조)와 도메인/예외 패턴을 정리해, 채팅 관련 기능을 추가/수정할 때 어디를 건드려야 하는지와 왜 이런 구조인지(빠른 응답 + 영속성 보장의 분리)를 파악하도록 한다. 인증 흐름은 [[auth-flow]] 문서를 참고.

## 사용 위치

- 채팅 메시지 전송/읽음 처리, 채팅방, 알림(뱃지) 관련 기능을 추가/수정할 때
- "메시지가 화면엔 보이는데 DB에 없다" 류의 비동기 영속화 이슈를 디버깅할 때
- user-server 유저 정보 캐시(`ChatUserInfo`)가 최신이 아닐 때

## 동작 방식

### 실시간 통신 — 3단 구조

`WebSocket(STOMP, 내장 SimpleBroker) ← Redis Pub/Sub(서버 인스턴스 간 브로드캐스트) ← Kafka(영속 저장/감사용 비동기 이벤트)`

`WebSocketConfig`(`@EnableWebSocketMessageBroker`): 내장 `SimpleBroker`(`/topic`, `/user`), 발행 prefix `/pub`, 엔드포인트 `/ws/chat`(SockJS fallback 포함). 단일 인스턴스 내 broker로는 다중 서버 인스턴스 간 메시지가 전달되지 않으므로, `RedisConfig`가 `chatroom`/`chatroom-read`/`unread-badge` 채널을 구독하고 `ChatRedisSubscriber`가 받은 메시지를 `SimpMessagingTemplate.convertAndSend("/topic/chats/{roomId}", ...)`로 WebSocket 클라이언트에 전달한다.

### 메시지 전송 흐름

`ChatWebSocketController.sendMessage()`(`/pub/chats/{roomId}`):

1. 메시지 ID(UUID) 생성, 발신자를 `readBy`에 즉시 기록(자동 읽음 처리)
2. `ChatRoomCacheService`로 참여자 수 조회(Redis 캐시 우선, 미스 시 DB 조회 후 TTL 24h 캐싱)
3. **DB 저장을 기다리지 않고** `ChatRedisPublisher.publish()`로 즉시 Redis 발행 → `ChatRedisSubscriber`가 STOMP로 broadcast (응답속도 우선)
4. `ChatKafkaProducer.send()`로 `chat-message-save` 토픽에 비동기 저장 이벤트 발행
5. `ChatKafkaConsumer.consumeMessage()`가 소비해 `ChatService.saveMessage()`로 MongoDB 영속화, 발신자 제외 수신자에게 `unread-badge` Redis 채널로 SSE 뱃지 갱신 신호 발행

읽음 처리(`PUT /rooms/{roomId}/messages/read`)는 동기 DB 갱신 + Redis 즉시 broadcast + Kafka 백업 이벤트(멱등 처리)로 이원화. Kafka Consumer는 실패 시 `DeadLetterPublishingRecoverer` + `FixedBackOff(1000ms, 3회)`로 `.DLT` 토픽 전송.

### 도메인 모델 (MongoDB, JPA 아님)

`ChatMessage`(`@Document chat_messages`): `roomId`, `senderUserId`, `content`, `type(TEXT/IMAGE)`, `readBy: Map<Long, Instant>`(사용자별 읽음 시각) — `markAsReadBy()`/`isReadBy()` 도메인 메서드. `ChatRoom`(`chat_rooms`): `participantUserIds`, `lastMessageAt`. `ChatUserInfo`(`chat_user_infos`, `@Id`가 userId): user-server 정보 캐시본, `deleted` 플래그로 소프트 삭제. `mapper` 패키지(`ChatMapper`)는 MyBatis가 아니라 MapStruct.

### user-server 연동

`UserFeignClient`(Spring Cloud OpenFeign, `GET /api/v1/users/{userId}`)는 `UserInfoCacheService.fetchAndSave()`에서 **MongoDB 캐시(`ChatUserInfo`) 미스 시에만 호출되는 fallback**이다. 평소에는 `user-info-updated` Kafka 토픽(`@KafkaListener`)으로 user-server 변경사항을 받아 MongoDB를 갱신한다. Feign 호출에는 인증 토큰이 전달되지 않는다. 자세한 내용은 [[architecture-overview]] 참고.

### migration — Mongock (MongoDB용 Flyway)

`migration/DatabaseChangelog.java`(order="001")가 `chat_rooms`/`chat_messages` 컬렉션을 초기 생성, `AddReadByFieldChangelog.java`(order="002")가 기존 메시지에 `readBy` 필드를 백필(rollback 포함). `mongock.migration-scan-package` 설정으로 스캔 패키지 지정.

### 예외 처리

user-server와 동일한 패턴: `GlobalRestControllerAdvice`/`GlobalViewControllerAdvice`가 `@RestController`/`@Controller` 어노테이션 기준으로 분리 처리. View는 `external.user-server.url/login`으로 리다이렉트.

### 로깅

user-server와 동일 구조(`CustomLogger`, `MDCHelper`, `JsonLogLayout`, `RequestLog`) — `RequestLoggingFilter`가 traceId를 주입하고 요청 종료 시 JSON 로그 발행.

### 주요 설정 (`application.yaml`)

포트 8082, MongoDB(`spring.data.mongodb.uri`), Redis 단일 인스턴스, Kafka, Mongock 스캔 패키지, MinIO(채팅 이미지), `external.user-server.url`(Feign 대상), `user.auth.*`/`user.presence.*`(user-server와 동일 prefix), `chatroom.metadata.*`.

## 주요 규칙

- 메시지의 "실시간 전달"과 "영속화"는 분리되어 있다 — Redis 발행이 먼저, Kafka를 통한 DB 저장은 비동기로 나중에 일어난다. 메시지 저장 로직을 수정할 때 이 순서를 깨뜨리지 않는다.
- 채팅 도메인은 MongoDB(Spring Data MongoDB)이며 JPA/MyBatis가 아니다.
- user-server 유저 정보는 항상 `ChatUserInfo` 캐시를 거쳐 조회한다 — Feign 직접 호출을 기본 경로로 추가하지 않는다.

## 주의사항

- DLT(Dead Letter Topic)로 빠진 메시지는 자동 재처리되지 않으므로, 메시지 손실 의심 시 `.DLT` 토픽을 확인해야 한다.
- Mongock 마이그레이션은 순서(`order`)에 의존하므로 새 changelog 추가 시 기존 order 번호와 충돌하지 않게 주의한다.

## 관련 코드

- `chat-server/src/main/java/com/project/blinddate/chat/config/WebSocketConfig.java`, `RedisConfig.java`, `KafkaConfig.java`
- `chat-server/src/main/java/com/project/blinddate/chat/controller/ChatWebSocketController.java`, `ChatController.java`
- `chat-server/src/main/java/com/project/blinddate/chat/service/ChatService.java`, `ChatKafkaProducer.java`, `ChatKafkaConsumer.java`, `ChatRedisPublisher.java`, `ChatRedisSubscriber.java`, `UserInfoCacheService.java`, `ChatRoomCacheService.java`
- `chat-server/src/main/java/com/project/blinddate/chat/domain/ChatMessage.java`, `ChatRoom.java`, `ChatUserInfo.java`
- `chat-server/src/main/java/com/project/blinddate/chat/migration/DatabaseChangelog.java`, `AddReadByFieldChangelog.java`
- `chat-server/src/main/java/com/project/blinddate/chat/external/user_client/UserFeignClient.java`