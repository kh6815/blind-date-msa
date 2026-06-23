# 인증/인가 흐름

## 목적

이 프로젝트는 Spring Security를 인가 게이트로 쓰지 않고, **AOP가 모든 컨트롤러 호출을 가로채 인증을 처리**하는 비표준 구조를 사용한다. 처음 보면 "왜 필터나 인터셉터가 아니라 AOP인가", "왜 SecurityConfig에 인증 로직이 없는가"가 혼란스러울 수 있어 문서화한다. 같은 패턴이 두 서버 모두에 반복되지만 토큰 검증 방식이 서로 다르므로 구분해서 이해해야 한다.

## 사용 위치

- 새 API 엔드포인트에서 "현재 로그인한 유저"가 누구인지 가져와야 할 때
- 인증 관련 버그(로그인은 되는데 인증 안 됨, 토큰은 있는데 401 등)를 디버깅할 때
- user-server와 chat-server 간 인증 상태 공유 방식을 변경해야 할 때

## 동작 방식

### 공통 구조

두 서버 모두 `@Around("@within(...Controller) || @within(...RestController)")` AOP가 모든 컨트롤러 메서드 호출을 가로챈다:

1. 요청에서 토큰 추출 — `Authorization` 헤더(`Bearer ` prefix) 또는 `Authorization` 쿠키(URL 디코딩)
2. 토큰 검증 후 userId 확보
3. 컨트롤러 메서드 파라미터 중 `UserIdRequest`(user-server) / `ChatUserIdRequest`(chat-server) 타입 인자를 실제 userId가 채워진 객체로 교체
4. `joinPoint.proceed(args)`로 교체된 인자와 함께 컨트롤러 실행

컨트롤러 입장에서는 `UserIdRequest userIdRequest` 같은 파라미터를 그냥 선언만 해두면(스프링 기본 바인딩으로는 `id=null`), AOP가 호출 직전에 실제 로그인 유저 id로 바꿔치기해 준다. `@LoginRequired` 같은 커스텀 어노테이션은 없다.

### user-server — JWT 서명 검증 방식

`user-server/src/main/java/com/project/blinddate/user/aop/UserActivityAspect.java:43-46`

```java
String token = jwtTokenProvider.resolveToken(request);
if (token != null && jwtTokenProvider.validateToken(token)) {
    Long userId = jwtTokenProvider.getUserId(token);
    ...
}
```

`JwtTokenProvider`(HS256)가 토큰 서명을 직접 검증하고 subject(userId)를 꺼낸다. Redis는 인증 자체에 쓰이지 않고, **presence(접속 상태) 갱신**에만 쓰인다 — `user:presence:{userId}` 키를 TTL 30분으로 갱신(line 47-49).

### chat-server — Redis 토큰 매핑 조회 방식

`chat-server/src/main/java/com/project/blinddate/chat/aop/ChatUserActivityAspect.java:58, 91, 131-134`

```java
Long currentUserId = resolveUserIdViaRedis(token);
```

chat-server는 JWT 서명을 검증하지 않는다. 대신 `user:token:{token}` 키를 Redis에서 직접 조회해 userId를 가져온다. 즉 **chat-server는 JWT secret을 모르며, user-server가 로그인 시 Redis에 적재한 토큰→userId 매핑을 그대로 신뢰**하는 구조다.

### 매핑이 만들어지는 시점

`user-server/src/main/java/com/project/blinddate/user/controller/ViewController.java` 로그인 처리(`login()`)에서:
- JWT 생성(`JwtTokenProvider.createToken`)
- Redis에 `user.auth.key-prefix`(`"user:token:"`) + 토큰을 키로, userId를 값으로 저장 (TTL: `user.auth.ttl-minutes`)
- 쿠키(`Authorization`, httpOnly, secure, sameSite=Lax)에 `Bearer {token}` 형태로 내려줌

두 서버의 `application.yaml`에 동일한 키 prefix(`user.auth.key-prefix: "user:token:"`, `user.presence.key-prefix: "user:presence:"`)가 정의되어 있어야 매핑이 일치한다.

## 주요 규칙

- Spring Security(`SecurityConfig`)는 인증을 처리하지 않는다 — CSRF/CORS/세션 정책(STATELESS) 설정과 `/actuator/**`, `/swagger-ui/**`의 사설 IP 대역(`10.0.0.0/8` 등) 제한만 담당한다.
- 실제 로그인 여부 판단은 항상 AOP(`UserActivityAspect` / `ChatUserActivityAspect`)를 거친다.
- 토큰이 없거나 무효하면: REST 컨트롤러는 인증 없이 그냥 진행(컨트롤러 파라미터가 채워지지 않음, 401은 명시적으로 던지는 곳에서만 발생)되거나, View 컨트롤러는 로그인 페이지로 리다이렉트된다. chat-server의 경우 REST는 401을, View는 `external.user-server.url/login`으로 리다이렉트한다.

## 주의사항

- chat-server는 토큰의 진위를 직접 검증할 수단이 없다 — Redis 매핑이 곧 인증의 근거이므로, Redis 키가 임의로 주입되면 인증을 우회할 수 있다(내부망 신뢰 전제).
- 두 서버의 `user.auth.key-prefix` / `user.presence.key-prefix` 값이 달라지면 인증 공유가 깨진다. 설정 변경 시 양쪽 `application.yaml`을 함께 확인해야 한다.
- AOP가 모든 컨트롤러를 가로채므로, 인증이 필요 없는 공개 API를 추가할 때도 이 AOP를 통과한다는 점을 인지해야 한다(토큰이 없으면 단순히 `UserIdRequest`가 채워지지 않을 뿐 요청 자체가 막히지는 않음).

## 관련 코드

- `user-server/src/main/java/com/project/blinddate/user/aop/UserActivityAspect.java`
- `user-server/src/main/java/com/project/blinddate/user/security/JwtTokenProvider.java`
- `user-server/src/main/java/com/project/blinddate/user/controller/ViewController.java`
- `chat-server/src/main/java/com/project/blinddate/chat/aop/ChatUserActivityAspect.java`
- `user-server/src/main/resources/application.yaml` (`user.auth.*`, `user.presence.*`)
- `chat-server/src/main/resources/application.yaml` (`user.auth.*`, `user.presence.*`)