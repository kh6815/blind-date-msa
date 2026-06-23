# user-server 모듈 구조

## 목적

user-server의 패키지별 역할과 패턴을 정리해, 새로운 기능을 추가할 때 어느 계층에 무엇을 작성해야 하는지, 기존 패턴(예외 처리, 도메인 모델, 로깅)을 어떻게 따라야 하는지 빠르게 파악하도록 한다. 인증 흐름은 [[auth-flow]] 문서를 참고.

## 사용 위치

- 회원가입/로그인/프로필/좋아요/추천 검색 등 user-server 도메인에 기능을 추가하거나 수정할 때
- 예외 처리, 도메인 모델, Kafka 이벤트 발행 패턴을 따라야 할 때

## 동작 방식

### 계층 구조

`controller` → `service`(`@Transactional`) → `repository`(JPA) / `mapper`(MapStruct, DTO↔Entity 변환). MyBatis는 사용하지 않는다 — `mapper` 패키지명에 속지 않아야 한다.

예시 흐름(위치 업데이트): `UserController.updateLocation()` → `UserService.updateLocation()` → `UserRepository.findById()` → `User.updateLocation()`(도메인 메서드, null 아닌 필드만 부분 갱신) → JPA dirty checking으로 트랜잭션 커밋 시 반영. 별도 `save()` 호출 없음.

### 도메인 모델 (JPA)

- `User`: `users` 테이블, `BaseEntity`(공통 모듈, `createdAt`/`updatedAt`/`delYn`/`deletedAt`, `softDelete()`/`restore()`) 상속. `updateProfile()`, `updateLocation()` 등 도메인 메서드로 변경을 캡슐화.
- `UserImage`: 유저 추가 이미지, `User`와 `@ManyToOne(LAZY)`.
- `UserLike`: 좋아요 기록, `actor`/`target` FK + 유니크 제약, `isRead` 플래그.
- `UserView`: 프로필 조회 기록, `viewer`/`viewed` FK + 유니크 제약.
- QueryDSL(`UserRepositoryImpl`)로 동적 검색 — 거리순 검색은 MySQL `ST_Distance_Sphere` 사용.

### 예외 처리

커스텀 예외 클래스가 없다 — 서비스 계층은 모든 비즈니스 예외를 `IllegalArgumentException`으로 던지고 메시지로 구분한다. `exception` 패키지에는 두 개의 어드바이스만 존재:

- `GlobalRestControllerAdvice`(`@RestControllerAdvice(annotations = RestController.class)`): `IllegalArgumentException`→400, validation 예외→400, 기타→500, 공통 `ResponseDto`로 응답.
- `GlobalViewControllerAdvice`(`@ControllerAdvice(annotations = Controller.class)`): 같은 예외를 잡아 flash 메시지 + 리다이렉트.

새 예외 상황을 추가할 때도 이 패턴(커스텀 예외 클래스 없이 `IllegalArgumentException` + 메시지)을 따른다.

### 로깅

`logger` 패키지가 구조화(JSON) 로깅을 담당:

- `MDCHelper`: `traceId`/`ipAddress`/`sql`/`debug`를 MDC에 적재, `onNewContext()`로 비동기 스레드에 부모 traceId 전파.
- `CustomLogger`: `CUSTOM_LOGGER` 전용 Logger, Gson + Logstash `StructuredArguments.fields()`로 JSON 로그 출력.
- `RequestLoggingFilter`: 매 요청마다 traceId 생성 → 요청/응답 바디·헤더 캡처 → `CustomLogger.info()`로 발행. SSE/actuator/정적 리소스/OPTIONS는 제외.
- `interceptor/CustomP6SpyLogger`: SQL 실행 로그를 같은 MDC 컨텍스트(`MDCHelper.appendSql()`)에 누적시켜, 한 요청의 JSON 로그에 SQL 메타데이터가 함께 포함되도록 연결.

### 외부 연동

- chat-server로의 이벤트 발행: `UserKafkaProducer`가 `user-info-updated` 토픽(3 파티션)으로 회원가입/정보수정/삭제 이벤트(`UserInfoEvent`) 발행.
- chat-server → user-server 방향 Kafka 리스너(`ChatMessageEventListener`)는 전체 주석 처리되어 **비활성** 상태.
- `external.chat-server.url` 설정 키는 존재하지만 코드에서 실제로 사용하는 곳은 없다.
- Redis Pub/Sub(`UserLikeRedisPublisher`/`Subscriber`)으로 같은 서비스 내 SSE(`UserSseController`)에 좋아요 알림 전달.

### 주요 설정 (`application.yaml`)

MySQL + Flyway(`baseline-on-migrate: true`, JPA `ddl-auto: validate`), Redis 단일 인스턴스, Kafka(`acks: all`, consumer group `user-group`), MinIO(프로필 이미지), JWT(`jwt.secret`, `jwt.expiration`), `user.auth.*`/`user.presence.*`(Redis 키 prefix/TTL), `web.cookie.domain`.

## 주요 규칙

- 서비스 계층 예외는 커스텀 클래스 없이 `IllegalArgumentException` + 메시지로 던진다.
- 도메인 객체 변경은 엔티티의 도메인 메서드(`updateProfile`, `updateLocation` 등)를 통해서만 한다 — 서비스가 필드를 직접 set하지 않는다.
- 컨트롤러 인증 정보는 `UserIdRequest` 파라미터로 받는다([[auth-flow]] 참고).

## 주의사항

- `mapper` 패키지를 MyBatis로 오해하지 않는다 — MapStruct다.
- chat-server 관련 변경 시 `ChatMessageEventListener`가 비활성 상태임을 먼저 확인한다.

## 관련 코드

- `user-server/src/main/java/com/project/blinddate/user/aop/UserActivityAspect.java`
- `user-server/src/main/java/com/project/blinddate/user/domain/User.java`, `UserImage.java`, `UserLike.java`, `UserView.java`
- `user-server/src/main/java/com/project/blinddate/user/exception/GlobalRestControllerAdvice.java`, `GlobalViewControllerAdvice.java`
- `user-server/src/main/java/com/project/blinddate/user/service/UserService.java`, `UserKafkaProducer.java`
- `user-server/src/main/java/com/project/blinddate/user/logger/CustomLogger.java`, `MDCHelper.java`
- `user-server/src/main/java/com/project/blinddate/common/BaseEntity.java`
- `user-server/src/main/resources/application.yaml`