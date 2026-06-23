# 아키텍처 개요

## 목적

blind-date-msa는 user-server와 chat-server 두 개의 독립 Spring Boot 서비스로 구성된 소개팅 앱 MSA다. 이 문서는 서비스 분리 기준, 각 서비스의 저장소/통신 기술, 서비스 간 연동 방식을 한눈에 파악할 수 있도록 정리한다. 개별 서비스의 세부 패턴은 [[user-server]], [[chat-server]] 문서를, 인증 흐름은 [[auth-flow]] 문서를, 배포/인프라는 [[infra-deployment]] 문서를 참고한다.

## 사용 위치

- 새 기능이 user-server/chat-server 중 어디에 속해야 하는지 판단할 때
- 서비스 간 연동(이벤트, 캐시, 호출) 구조를 변경하거나 디버깅할 때
- 전체 요청 흐름을 추적해야 할 때 (예: "좋아요 알림이 왜 안 오지" 같은 크로스 서비스 이슈)

## 동작 방식

### 서비스 구성

| 서비스 | 포트 | 주 저장소 | 역할 |
|---|---|---|---|
| user-server | 8081 | MySQL(JPA) | 회원가입/로그인, 프로필, 좋아요, 추천 검색 |
| chat-server | 8082 | MongoDB | 채팅방, 메시지, 실시간 알림(SSE 뱃지) |

공유 인프라: Redis(인증 세션·presence·pub/sub 공용 단일 인스턴스), Kafka(비동기 이벤트 버스), MinIO(이미지 스토리지, 양쪽 서버가 각자 버킷 사용).

### 서버 간 통신 — 두 가지 경로가 공존

1. **Kafka 비동기 이벤트 (주 경로)**: user-server가 회원 정보 변경 시 `user-info-updated` 토픽으로 이벤트를 발행하고, chat-server가 이를 구독해 MongoDB의 `chat_user_infos` 컬렉션(유저 정보 캐시본)에 반영한다. chat-server → user-server 방향의 Kafka 리스너(`ChatMessageEventListener`)도 user-server 쪽에 존재하지만 현재 전체 주석 처리되어 **비활성 상태**다.
2. **Feign 동기 호출 (보조/fallback 경로)**: chat-server의 `UserFeignClient`가 `GET /api/v1/users/{userId}`를 호출할 수 있지만, 실제로는 MongoDB 캐시(`ChatUserInfo`)가 미스일 때만 호출되는 fallback이며 평소에는 Kafka 이벤트로 캐시가 최신 상태를 유지하므로 거의 호출되지 않는다. 이 호출에는 인증 토큰이 전달되지 않는다.

이 구조 때문에 chat-server는 user-server의 DB에 직접 접근하지 않고, 항상 자신의 MongoDB 캐시(`ChatUserInfo`)를 거쳐 유저 정보를 조회한다.

### 인증 상태 공유

두 서버는 JWT secret을 공유하는 것이 아니라, **로그인 시 user-server가 Redis에 적재하는 `user:token:{token}` → userId 매핑을 같은 Redis 인스턴스에서 조회**해 인증 상태를 공유한다. 단, 검증 방식 자체는 두 서버가 다르다 — 자세한 내용은 [[auth-flow]] 참고.

### 실시간 기능

- chat-server: WebSocket(STOMP) + Redis Pub/Sub(다중 인스턴스 브로드캐스트) + Kafka(비동기 영속화) 3단 구조. 자세한 내용은 [[chat-server]] 참고.
- user-server: 좋아요 알림을 Redis Pub/Sub → SSE로 전달.

### 다중 인스턴스 구조

로컬/기본 환경(`docker-compose.yml`)은 user-server, chat-server 각 2개 인스턴스(`-1`, `-2`)를 nginx가 `least_conn`으로 로드밸런싱한다. 운영 환경(오라클 클라우드, `docker-compose-prod.yml`)은 리소스 제약으로 각 1개 인스턴스만 운영 중이다. 자세한 내용은 [[infra-deployment]] 참고.

## 주요 규칙

- 한 서비스가 다른 서비스의 DB를 직접 조회하지 않는다 — 항상 캐시(MongoDB `ChatUserInfo`) 또는 이벤트(Kafka)를 거친다.
- 인증/인가는 Spring Security가 아니라 AOP(`UserActivityAspect`, `ChatUserActivityAspect`)가 컨트롤러 호출을 가로채는 방식으로 공통 처리된다. 두 서버 모두 동일한 패턴을 따른다.
- `mapper` 패키지명은 두 서버 모두 MyBatis가 아니라 MapStruct(DTO↔도메인 변환)를 의미한다. SQL 매핑과 무관하다.

## 주의사항

- chat-server → user-server 방향 Kafka 리스너는 코드상 존재하지만 비활성(주석 처리)이므로, "왜 반영이 안 되지"라는 이슈가 있다면 이 부분을 먼저 의심해야 한다.
- Feign 호출 경로는 인증이 없으므로 외부 노출 시 보안 검토가 필요하다(현재는 내부망 전용으로 가정).

## 관련 코드

- `user-server/src/main/java/com/project/blinddate/user/service/UserKafkaProducer.java`
- `chat-server/src/main/java/com/project/blinddate/chat/service/UserInfoCacheService.java`
- `chat-server/src/main/java/com/project/blinddate/chat/external/user_client/UserFeignClient.java`
- `user-server/src/main/java/com/project/blinddate/user/service/ChatMessageEventListener.java` (비활성)