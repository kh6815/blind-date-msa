# 인프라 및 배포 구조

## 목적

docker-compose, nginx, 모니터링 스택 설정이 로컬/운영 환경에서 어떻게 다른지, 왜 다른지(리소스 제약, 클라우드 전환)를 정리해, 배포 설정을 변경할 때 두 환경 중 어디를 고쳐야 하는지 헷갈리지 않도록 한다.

## 사용 위치

- `docker-compose.yml` / `docker-compose-prod.yml` / `nginx/*.conf`를 수정할 때
- 운영 환경에 인스턴스를 추가/제거하거나 도메인·인증서 설정을 변경할 때
- 모니터링(Prometheus/Grafana/Loki/Alloy) 대시보드나 수집 경로를 다룰 때

## 동작 방식

### 로컬/기본 환경 (`docker-compose.yml`)

- 저장소: `mysql-user`, `mongo-chat`, `minio`. 캐시/메시징: `redis`(단일), `zookeeper`+`kafka`(단일 브로커).
- 애플리케이션: **user-server-1/2, chat-server-1/2 — 각 서비스 2개 인스턴스**가 명시적으로 정의되어 있다(`logs/user-server-1,2`, `logs/chat-server-1,2` 디렉토리와 1:1 대응). 각 컨테이너는 자체 Dockerfile에서 직접 빌드(`build.context`).
- nginx는 `app-net` 네트워크에서 `user.blind-date.com`, `chat.blind-date.com`, `minio.blind-date.com` 별칭으로 연결.
- 모니터링(prometheus/alloy/loki/grafana)도 같은 compose 파일에 포함.
- Redis Cluster(6노드), Kafka Cluster(3브로커) 정의는 **주석 처리되어 미사용** — README에 "테스트·학습 목적, 오버스펙으로 실제 배포 미적용"이라 명시.

### 운영 환경 (`docker-compose-prod.yml`, 오라클 클라우드)

로컬 환경과의 주요 차이점:

- **인스턴스 단일화**: `user-server-2`, `chat-server-2`가 전부 주석 처리 — `-1`만 운영. 클라우드 인스턴스 리소스 제약 때문으로 추정.
- **빌드 방식 변경**: `build.context` 대신 사전 빌드된 `./user-server.jar`, `./chat-server.jar`를 볼륨 마운트하고 `amazoncorretto:17-alpine` 베이스로 `java -jar` 실행.
- **Kafka 리소스 축소**: `KAFKA_LOG_RETENTION_HOURS: 6`, `KAFKA_LOG_RETENTION_BYTES: 500MB`, `KAFKA_HEAP_OPTS: "-Xms256m -Xmx512m"`.
- **인증서**: `./cert`(mkcert, 로컬용) 대신 `/etc/letsencrypt:/etc/letsencrypt:ro`(Let's Encrypt) 마운트.
- **도메인 전환**: `.com` → `.site`(예: `user.blind-date.site`), `grafana.blind-date.site` 별칭 추가(단 서버 블록은 주석 처리).
- **모니터링 스택 비활성**: prometheus/alloy/loki/grafana 서비스 전체가 주석 처리 — 운영 환경에서 별도 운용 중인지, 아직 미적용인지는 compose 파일만으로 확인 불가.
- 모든 서비스에 `x-logging` 앵커로 `json-file` 드라이버(`max-size: 10m`, `max-file: 3`) 공통 적용.

> 민감정보(`MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `MINIO_ACCESS_KEY` 등)는 두 compose 파일 모두 `${VAR}` 형태로 외부 `.env`에서 주입하며 키 이름만 노출된다.

### Nginx (`nginx/nginx.conf` vs `nginx/nginx-prod.conf`)

- `upstream user_upstream`/`chat_upstream`이 `least_conn` 알고리즘으로 로드밸런싱. 기본 conf는 `-1`/`-2` 두 서버 등록, prod conf는 `-2`가 주석 처리되어 단일 서버 라우팅(compose 인스턴스 단일화와 일치). `proxy_next_upstream` + `max_fails=1 fail_timeout=10s`로 장애 노드 일시 제외.
- 라우팅: `user.blind-date.{com,site}` → `/api/v1/users/`, `/`를 `user_upstream`. `chat.blind-date.{com,site}` → `/api/v1/chats/`, `/ws/chat`, `/`를 `chat_upstream`. `minio.blind-date.{com,site}` → `minio_upstream`.
- **WebSocket**: `/ws/chat`에 `Upgrade $http_upgrade`, `Connection "upgrade"`, `proxy_read_timeout 3600`.
- **SSE**: `/api/v1/users/sse/like-stream`, `/api/v1/chats/sse/badge-stream`에 `proxy_buffering off`, `proxy_cache off`, `proxy_read_timeout 86400s`, `X-Accel-Buffering: no`, `keepalive_timeout 7200`.
- TLS: 기본 conf는 `cert/`(mkcert), prod conf는 Let's Encrypt 경로(`/etc/letsencrypt/live/...`). 도메인 전환(`.com`→`.site`)과 함께 인증서 발급 방식도 변경됨.

### 모니터링 스택

- **메트릭**: `prometheus.yml`이 `user-server-1/2:8081`, `chat-server-1/2:8082`의 `/actuator/prometheus`를 5초 간격 스크랩 → Prometheus TSDB 저장 → Grafana 시각화.
- **로그**: `alloy/config/config.alloy`가 `/var/log/app/*/*.log`(compose `./logs` 마운트)를 tail → 멀티라인 병합 → JSON 파싱(`timestamp`/`level`/`logger`/`message`) → `loki.write`로 Loki 푸시. Loki는 TSDB 스키마 v13, filesystem 스토리지, 7일 보관(`retention_period: 168h`).
- Grafana 데이터소스 설정 파일(`grafana/setting/datasources/defalut.yml`)에는 Prometheus만 명시되어 있고 Loki 데이터소스는 빠져 있음(UI에서 별도 추가했을 가능성). 알림 규칙은 `grafana/setting/alerting/alert-rules.yaml`.

### cert/ 디렉토리

`cert.pem`, `key.pem`, `rootCA.pem` — mkcert로 생성한 로컬 개발용 TLS 인증서. `nginx.conf`(로컬용)에서만 사용하며, prod 환경은 Let's Encrypt를 쓰므로 이 디렉토리와 무관하다.

## 주요 규칙

- 로컬 compose와 prod compose는 별개 파일로 관리되며, 인스턴스 수·빌드 방식·도메인·인증서가 모두 다르다 — 한쪽만 고치고 다른 쪽을 잊지 않도록 주의.
- 인스턴스를 추가/제거할 때는 `docker-compose*.yml`과 `nginx/nginx*.conf`의 upstream 블록을 함께 수정해야 한다.

## 주의사항

- README.md는 아직 "AWS EC2 배포" 기준으로 작성되어 있으나 최근 커밋(`47a523f 오라클 클라우드로 전환`)으로 실제 배포 환경이 바뀌었다 — README 갱신이 누락된 상태이므로 인프라 관련 안내 시 이 점을 감안한다.
- 운영 환경의 모니터링 스택(prometheus/grafana/loki/alloy) 활성화 여부는 compose 파일만으로 단정할 수 없다 — 실제 운영 서버에서 별도 확인이 필요하다.

## 관련 코드

- `docker-compose.yml`, `docker-compose-prod.yml`
- `nginx/nginx.conf`, `nginx/nginx-prod.conf`
- `monitoring/prometheus/config/prometheus.yml`
- `monitoring/alloy/config/config.alloy`
- `monitoring/loki/config/loki.yml`
- `monitoring/grafana/setting/`