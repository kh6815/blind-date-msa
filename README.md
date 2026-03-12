# Blind Date – 소개팅 실시간 매칭 & 채팅 MSA 백엔드

실시간 소개팅 매칭과 채팅 기능을 제공하는 **Spring Boot MSA 멀티 모듈 백엔드** 프로젝트입니다.  
`user-server`와 `chat-server`를 분리하고, Kafka 이벤트와 Redis·MongoDB·MySQL·MinIO를 활용해 **느슨한 결합 + 수평 확장**이 가능한 구조로 설계했습니다.

---

## 전체 아키텍처 개요

![blind-date 아키텍처.jpg](blind-date%20%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98.jpg)
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

---

## 인프라 & 데브옵스

### 컨테이너 오케스트레이션 (`docker-compose.yml`)

- **인프라 서비스**
  - `mysql-user` (MySQL 8)
  - `mongo-chat` (MongoDB 7)
  - `redis` (Redis 7, AOF)
  - `zookeeper` + `kafka` (Confluent 이미지)
  - `minio` (MinIO 오브젝트 스토리지)
- **애플리케이션 서비스**
  - `user-server-1`, `user-server-2`
    - 동일 이미지로 **수평 확장(2 인스턴스)**, Health Check(헬스 엔드포인트 기반) 설정
  - `chat-server-1`, `chat-server-2`
    - Mongo/Redis/Kafka/MinIO 의존성으로 구성, Health Check 설정
  - `nginx`
    - HTTPS Termination (mkcert로 발급한 로컬 Root CA/서버 인증서 사용)
    - 도메인별 Reverse Proxy 및 로드밸런싱
- **모니터링 서비스**
  - `prometheus` (각 Spring Boot 인스턴스 메트릭 스크랩)
  - `grafana` (대시보드 시각화, 4000 포트 매핑)

환경 변수(`.env`)를 통해 DB/Redis/Kafka/MinIO/JWT 등 설정 값을 주입하며,  
각 컨테이너는 `app-net` 브리지 네트워크로 연결되어 마이크로서비스 간 통신을 분리합니다.

---

## 모니터링 & 로깅 (`monitoring/`)

- **Prometheus**
  - `monitoring/prometheus/config/prometheus.yml` 에서 User/Chat 서버 Actuator 메트릭 엔드포인트를 스크랩
- **Grafana**
  - 기본 데이터소스/대시보드 설정 파일 제공
  - Spring Boot용 시스템 모니터링 대시보드 예시 포함
- **Loki + Alloy**
  - Alloy 에이전트가 애플리케이션 로그 디렉터리(`logs/`)를 tail 하여 Loki에 전송
  - Grafana에서 로그·메트릭을 함께 조회할 수 있는 **Observability 스택** 구성

---

## 📊 결과 및 성과

- **구현한 것**  
  - 유저·채팅 도메인 분리 MSA, Kafka 기반 이벤트 연동  
  - MySQL + MongoDB + Redis + MinIO를 도메인별로 나눈 저장소 구성  
  - Docker Compose로 User/Chat 서버 각 2대 수평 확장 + Nginx 로드밸런싱  
  - Prometheus + Grafana + Loki로 메트릭·로그 통합 관찰  
- **해결한 문제**  
  - 서버 간 **직접 HTTP 호출 대신 Kafka 이벤트**로 전환해 결합도를 낮추고, 채팅 서버 장애가 유저 API에 바로 전파되지 않도록 설계했습니다.  
  - 채팅 메시지처럼 **대량·비정형** 데이터는 MongoDB, **정합성이 중요한** 유저·매칭은 MySQL로 나누어 적재적소에 DB를 선택했습니다.  
- **정량 수치 (가능하면 채워 주세요)**  
  - 예: “API 00개 구현”, “단위·통합 테스트 00개”, “도커 컨테이너 00개로 일괄 실행” 등

---

## 실행 방법
- **전체 인프라 + 애플리케이션 Docker 실행**
  - 주요 서비스만 실행:  
    `docker compose up -d --build user-server-1 user-server-2 chat-server-1 chat-server-2`
  - 인프라/모니터링 포함 전체 실행:  
    `docker compose up -d`
  






### 향후 계획

- Redis 기반 분산 락으로 선점·결제 등 동시성 구간 보강  
- CI/CD 파이프라인(GitHub Actions 등) 도입  
- Kubernetes 등으로 배포 환경 확장 검토  
