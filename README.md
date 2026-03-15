# Blind Date – 소개팅 실시간 매칭 & 채팅 MSA 백엔드

**Blind-Date**는 주변에 있는 사람들을 기반으로 새로운 인연을 만날 수 있는 소개팅 앱입니다.

내 주변의 가까운 사용자 목록을 확인하고 관심 있는 사람과 바로 매칭할 수 있습니다.
매칭된 상대와 실시간 채팅을 통해 자연스럽게 대화를 시작할 수 있습니다.
---

## 전체 아키텍처 개요

`user-server`와 `chat-server`를 분리하고, Kafka 이벤트와 Redis·MongoDB·MySQL·MinIO를 활용해 **느슨한 결합 + 수평 확장**이 가능한 구조로 설계했습니다.
![blind-date 아키텍처.jpg](img/blind-date%20%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98.jpg)

---

## 🛠 기술 스택

| 구분 | 기술 | 용도 |
|------|------|------|
| **언어·프레임워크** | Java 17, Spring Boot 3.x | 백엔드 API·비즈니스 로직 |
| **아키텍처** | MSA, 멀티 모듈(Gradle) | 유저 서버 / 채팅 서버 분리 |
| **DB** | MySQL 8, MongoDB 7 | 유저·매칭(정합성) / 채팅 메시지(대량·유연 스키마) |
| **캐시·메시지** | Redis 7, Apache Kafka | 세션·캐시·실시간성 / 서버 간 이벤트 비동기 전달 |
| **파일 저장** | MinIO | 프로필·채팅 이미지 오브젝트 스토리지 |
| **실시간 통신** | WebSocket (STOMP) | 채팅 실시간 송수신 |
| **인증** | Spring Security, JWT | 로그인·API 인증 |
| **검색·쿼리** | QueryDSL, Spring Data JPA | 복잡 조회·타입 세이프 쿼리 |
| **API 문서** | Springdoc OpenAPI (Swagger) | API 명세·테스트 |
| **인프라·배포** | Docker, Docker Compose, Nginx | 컨테이너화, HTTPS·로드밸런싱, 수평 확장 |
| **모니터링·로깅** | Prometheus, Grafana, Loki, Alloy | 메트릭 수집·대시보드·로그 수집·통합 조회 |


- **루트 프로젝트 (`blind-date`)**
  - Gradle 멀티 모듈 관리 (`build.gradle`, `settings.gradle`)
  - 공통 Java 17 Toolchain, JUnit5 테스트 설정

- **`user-server`**
  - 역할: **유저/프로필/인증** 도메인 API 서버
  - 주요 기능
    - 회원 가입/로그인/로그아웃 (JWT 기반 인증)
    - 프로필 관리 (MinIO 연동 이미지 업로드)
    - 유저 정보/매칭 상태 조회 API
    - Kafka Producer를 통한 **채팅 서버와의 이벤트 연동**
  - 주요 기술 스택
    - Spring Boot Web, Security, Validation, Thymeleaf
    - Spring Data JPA + QueryDSL (MySQL)
    - Spring Data Redis
    - Flyway(DB 마이그레이션)
    - Spring Kafka
    - MinIO SDK
    - MapStruct, Lombok
    - Springdoc OpenAPI (API 문서화)
    - Micrometer + Prometheus (모니터링)
    - Logstash Logback Encoder, P6Spy (SQL/애플리케이션 로그)

- **`chat-server`**
  - 역할: **실시간 채팅/채팅방** 도메인 서버
  - 주요 기능
    - WebSocket 기반 실시간 1:1 채팅
    - MongoDB에 채팅 메시지·채팅방 히스토리 저장
    - Kafka를 통한 채팅 메시지 저장 비동기 처리
    - Redis Pub/Sub을 이용한 채팅 시스템 및 유저 캐싱
  - 주요 기술 스택
    - Spring Boot Web, Security, WebSocket, Validation, Thymeleaf
    - Spring Data MongoDB, Spring Data Redis
    - Mongock (MongoDB 마이그레이션)
    - Spring Kafka
    - MinIO SDK
    - MapStruct, Lombok
    - Springdoc OpenAPI
    - Micrometer + Prometheus
---

## 프로젝트 구조

<pre>
blind-date/
├── user-server/                    # 유저 도메인 API 서버 (MySQL, JPA, Kafka, MinIO)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/project/blinddate/
│   │   │   │   ├── common/         # API 경로 상수, 공통 DTO(ResponseDto 등)
│   │   │   │   ├── user/
│   │   │   │   │   ├── config/     # Security, Kafka, MinIO, QueryDSL, CORS 등 설정
│   │   │   │   │   ├── controller/ # REST API · 뷰(Thymeleaf) 컨트롤러
│   │   │   │   │   ├── domain/     # JPA Entity (User, UserImage)
│   │   │   │   │   ├── dto/        # 요청·응답 DTO
│   │   │   │   │   ├── exception/  # 전역 예외 처리 (RestControllerAdvice 등)
│   │   │   │   │   ├── filter/     # 요청 로깅 필터
│   │   │   │   │   ├── logger/     # 커스텀 로거, MDC, JSON 로그 레이아웃
│   │   │   │   │   ├── mapper/     # MapStruct 매퍼 (Entity ↔ DTO)
│   │   │   │   │   ├── repository/ # JPA Repository, QueryDSL 구현체
│   │   │   │   │   ├── security/   # JWT 토큰 발급·검증
│   │   │   │   │   ├── service/    # 비즈니스 로직 (유저, 이미지, Kafka 발행 등)
│   │   │   │   │   └── aop/        # 활동 로깅 등 AOP
│   │   │   │   └── UserServerApplication.java
│   │   │   └── resources/
│   │   │       ├── db/migration/   # Flyway 마이그레이션 스크립트
│   │   │       ├── templates/      # Thymeleaf HTML (로그인, 프로필, 홈 등)
│   │   │       ├── static/         # CSS, JS
│   │   │       ├── application.yaml
│   │   │       └── logback.xml
│   │   └── test/java/              # 단위·통합 테스트
│   └── build.gradle
│
├── chat-server/                    # 채팅 도메인 API · WebSocket 서버 (MongoDB, Redis, Kafka)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/project/blinddate/
│   │   │   │   ├── common/         # API 경로 상수, 공통 DTO
│   │   │   │   ├── chat/
│   │   │   │   │   ├── config/     # Security, MongoDB, Redis, Kafka, WebSocket, CORS 등
│   │   │   │   │   ├── controller/ # REST API · WebSocket · 뷰 컨트롤러
│   │   │   │   │   ├── domain/     # MongoDB Document (ChatRoom, ChatMessage, MessageType)
│   │   │   │   │   ├── dto/        # 요청·응답 DTO
│   │   │   │   │   ├── exception/  # 전역 예외 처리
│   │   │   │   │   ├── external/   # User 서버 Feign 클라이언트
│   │   │   │   │   ├── filter/     # 요청 로깅 필터
│   │   │   │   │   ├── logger/     # 커스텀 로거, MDC, JSON 로그
│   │   │   │   │   ├── mapper/     # MapStruct 매퍼
│   │   │   │   │   ├── migration/  # Mongock DB 변경 로그
│   │   │   │   │   ├── repository/ # MongoDB Repository
│   │   │   │   │   ├── service/    # 채팅방·메시지·Kafka 구독·Redis Pub/Sub 등
│   │   │   │   │   └── aop/        # 채팅 활동 로깅 및 캐싱 AOP
│   │   │   │   └── ChatServerApplication.java
│   │   │   └── resources/
│   │   │       ├── templates/     # Thymeleaf (채팅방, 채팅 목록 등)
│   │   │       ├── static/         # CSS, JS
│   │   │       ├── application.yaml
│   │   │       └── logback.xml
│   │   └── test/java/              # 단위·통합 테스트
│   └── build.gradle
│
├── monitoring/                     # Prometheus, Grafana, Loki, Alloy 설정
├── nginx/                          # Nginx 설정 (HTTPS, 리버스 프록시)
├── docker-compose.yml
├── build.gradle
└── settings.gradle
</pre>

---

## 각 서버 ERD
![서버 ERD.jpg](img/%EC%84%9C%EB%B2%84%20ERD.jpg)
### User Server (MySQL)
**정합성이 중요한** 유저·매칭은 MySQL DB를 선택했습니다.

유저·프로필·프로필 이미지를 관리합니다. `users` 1 : N `user_images` 관계입니다.

| 테이블 | 설명 |
|--------|------|
| **users** | 회원 정보 (이메일, 비밀번호 해시, 닉네임, 성별, 생년월일, MBTI, 관심사, 프로필 이미지 URL, 직업, 소개, 위치·위도·경도) |
| **user_images** | 유저별 추가 이미지 (user_id로 users 참조, CASCADE 삭제) |

---

### Chat Server (MongoDB)
채팅 메시지처럼 **대량·비정형** 데이터는 MongoDB를 선택했습니다.  

채팅방·채팅 메시지를 Document로 저장합니다. `chat_rooms` 1 : N `chat_messages` (roomId로 논리적 참조)입니다.

| 컬렉션 | 설명 |
|--------|------|
| **chat_rooms** | 채팅방 (참여자 user id 목록, 생성 시각, 마지막 메시지 시각) |
| **chat_messages** | 채팅 메시지 (roomId로 채팅방 참조, 발신자 ID, 내용, 타입(TEXT/IMAGE), 전송 시각) |

---

## 인프라 & 데브옵스

### 컨테이너 오케스트레이션 (`docker-compose.yml`)

`docker-compose.yml` 한 파일로 **DB·캐시·메시지브로커·앱·모니터링**을 일괄 기동합니다.  
모든 서비스는 `app-net`(bridge, 이름 `blind-date-app-net`) 하나의 네트워크에 올라가며, 환경 변수는 `.env`로 주입합니다.

---

#### 1. 데이터 저장소

| 서비스 | 이미지 | 설명 |
|--------|--------|------|
| **mysql-user** | `mysql:8.0` | User 서버 전용 DB. `utf8mb4`/`utf8mb4_unicode_ci` 고정. 포트 3306 노출, 볼륨 `mysql-user-data`. DB명·계정은 `MYSQL_DATABASE`, `SPRING_DATASOURCE_*` 등 환경 변수 사용. |
| **mongo-chat** | `mongo:7.0` | Chat 서버 전용 MongoDB. `MONGO_INITDB_DATABASE`로 DB 초기화. 포트 27017 노출, 볼륨 `mongo-chat-data`. |
| **minio** | `minio/minio:latest` | 오브젝트 스토리지(프로필·채팅 이미지). `server /data --console-address ":9001"`. 볼륨 `minio-data`, 인증은 `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY`. |

---

#### 2. Redis Cluster (6노드 · 3 Master + 3 Replica)

| 서비스 | 설명 |
|--------|------|
| **redis-node-1 ~ redis-node-6** | `redis:7.2`, `--cluster-enabled yes`. AOF(`appendonly yes`, `appendfsync everysec`) + RDB 스냅샷(`save 900 1`, `300 10`, `60 10000`)으로 이중 복구. 각 노드별 Health Check: `redis-cli ping` (5초 간격). 노드당 전용 볼륨 `redis-node-1-data` ~ `redis-node-6-data`. |
| **redis-cluster-init** | `restart: "no"`로 **최초 1회만** 실행. `redis-node-1~6`가 모두 healthy 된 뒤 `redis-cli --cluster create ... --cluster-replicas 1 --cluster-yes`로 3 master / 3 replica 클러스터 구성. `condition: service_completed_successfully`로 앱 서버는 이 완료 후 기동. |

애플리케이션에는 `SPRING_DATA_REDIS_CLUSTER_NODES`로 6노드 주소를 넘겨 Redis Cluster 모드로 접속합니다.

---

#### 3. Kafka Cluster (3 Broker)

| 서비스 | 설명 |
|--------|------|
| **zookeeper** | `confluentinc/cp-zookeeper:7.6.1`. 클라이언트 포트 2181, Kafka 브로커들의 메타데이터·리더 선출용. |
| **kafka-1, kafka-2, kafka-3** | `confluentinc/cp-kafka:7.6.1`. 각각 `KAFKA_BROKER_ID` 1~3, `KAFKA_ADVERTISED_LISTENERS`로 `kafka-1:9092` 등 개별 노드명 노출. `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=3`, `KAFKA_DEFAULT_REPLICATION_FACTOR=3`, `KAFKA_MIN_INSYNC_REPLICAS=2`로 가용성·내구성 확보. Health Check: `kafka-broker-api-versions --bootstrap-server localhost:9092` (10초 간격, 30s start_period). |

앱 서버는 `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-1:9092,kafka-2:9092,kafka-3:9092`로 3브로커에 연결하며, User/Chat 서버는 **Kafka가 healthy 된 뒤**(`condition: service_healthy`) 기동합니다.

---

#### 4. 게이트웨이 (Nginx)

| 항목 | 내용 |
|------|------|
| **이미지** | `nginx:1.25-alpine` |
| **의존성** | `user-server-1`, `user-server-2`, `chat-server-1`, `chat-server-2` 기동 후 시작 |
| **포트** | `80`, `443` 호스트 노출 |
| **볼륨** | `./nginx/nginx.conf` → `/etc/nginx/nginx.conf`, `./cert` → `/etc/nginx/ssl` (HTTPS 인증서) |
| **네트워크 별칭** | `user.blind-date.site`, `chat.blind-date.site`, `minio.blind-date.site` (같은 app-net 내에서 도메인별 라우팅) |

HTTPS 종료 및 User/Chat/MinIO 도메인별 리버스 프록시·로드밸런싱을 담당합니다.

---

#### 5. 애플리케이션 서버 (수평 확장)

| 서비스 | 빌드 | 포트 | 의존성(기동 조건) | Health Check | 로그 볼륨 |
|--------|------|------|-------------------|--------------|-----------|
| **user-server-1, user-server-2** | `user-server/Dockerfile` | 8081 | `mysql-user`(started), `redis-cluster-init`(completed_successfully), `kafka-1`(healthy), `minio`(started) | `GET http://127.0.0.1:8081/api/v1/users/health` (10s 간격, 20s start_period) | `./logs/user-server-1`, `./logs/user-server-2` → `/app/logs` |
| **chat-server-1, chat-server-2** | `chat-server/Dockerfile` | 8082 | `mongo-chat`(started), `redis-cluster-init`(completed_successfully), `kafka-1`(healthy), `minio`(started) | `GET http://127.0.0.1:8082/api/v1/chats/health` (동일 주기) | `./logs/chat-server-1`, `./logs/chat-server-2` → `/app/logs` |

- **User 서버**  
  `SPRING_PROFILES_ACTIVE=docker`, MySQL URL/계정, Redis Cluster 6노드, Kafka 3브로커, MinIO/JWT/쿠키 도메인 등 환경 변수로 주입.
- **Chat 서버**  
  MongoDB URI, Redis Cluster 6노드, Kafka 3브로커, MinIO/쿠키 도메인, User 서버 URL(`EXTERNAL_USER_SERVER_URL`) 등 환경 변수로 주입.

동일 이미지로 **User 2대, Chat 2대** 수평 확장하며, Nginx가 이 4개 인스턴스로 트래픽을 분산합니다.

---

#### 6. 모니터링 · 로그 수집

| 서비스 | 이미지 | 역할 |
|--------|--------|------|
| **prometheus** | `prom/prometheus:latest` | `./monitoring/prometheus/config/prometheus.yml` 마운트, 스크랩 결과는 `./monitoring/prometheus/data`에 저장. User/Chat 서버 Actuator 메트릭 수집. |
| **loki** | `grafana/loki:3.5.1` | `./monitoring/loki/config`, `./monitoring/loki/data` 마운트. 로그 저장소. |
| **alloy** | `grafana/alloy:latest` | `./monitoring/alloy/config/config.alloy` 설정으로 `./logs`(호스트) → `/var/log/app` 마운트된 로그 파일을 tail 하여 Loki로 전송. `depends_on: loki`. |
| **grafana** | `grafana/grafana:latest` | `depends_on: prometheus`, `loki`. 포트 `4000:3000` 노출. 대시보드·데이터소스는 `./monitoring/grafana/data`에 유지. |

메트릭(Prometheus)과 로그(Loki + Alloy)를 Grafana 한 곳에서 조회할 수 있도록 구성되어 있습니다.

---

#### 7. 볼륨 · 네트워크 요약

- **Named Volumes**  
  `mysql-user-data`, `mongo-chat-data`, `redis-node-1-data` ~ `redis-node-6-data`, `minio-data`.  
  컨테이너 삭제 시에도 데이터는 유지됩니다.
- **네트워크**  
  `app-net` (driver: bridge, name: `blind-date-app-net`).  
  위 모든 서비스가 이 네트워크에 연결되어 서비스명(호스트명)으로 통신합니다.

---

## 모니터링 & 로깅 (`monitoring/`)

- **Prometheus**
  - `monitoring/prometheus/config/prometheus.yml` 에서 User/Chat 서버 Actuator 메트릭 엔드포인트를 스크랩
- **Alloy + Loki**
  - Alloy 에이전트가 애플리케이션 로그 디렉터리(`logs/`)를 tail 하여 Loki에 전송
  - Grafana에서 로그·메트릭을 함께 조회할 수 있는 **Observability 스택** 구성
- **Grafana**
    - 기본 데이터소스/대시보드 설정 파일 제공
    - Spring Boot용 시스템 모니터링 대시보드 예시 포함

---

## 📊 결과

- **구현한 것**  
  - 유저·채팅 도메인 분리 MSA, Redis 캐싱, Kafka 기반 이벤트 연동  
  - MySQL + MongoDB + MinIO를 도메인별로 나눈 저장소 구성  
  - Docker Compose로 User/Chat 서버 각 2대 수평 확장 + Nginx 로드밸런싱  
  - Prometheus + Grafana + Loki로 메트릭·로그 통합 관찰  

---

### 향후 계획

- 좋아요 기능, 알림 등 추가 기능 개발
- Redis 기반 분산 락으로 선점·결제 등 동시성 구간 보강  
- CI/CD 파이프라인(GitHub Actions 등) 도입  
- Kubernetes 등으로 배포 환경 확장 검토  
