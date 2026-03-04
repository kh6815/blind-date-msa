## Blind Date MSA 백엔드 구조

소개팅 서비스의 백엔드를 MSA 멀티 모듈 구조로 구성한 프로젝트입니다.

- **`common-core`**: 공통 DTO, 상수, 유틸리티 모듈  
- **`user-server`**: 유저 도메인 API 서버 (MySQL, JPA, Redis, Kafka, MinIO 연동)  
- **`chat-server`**: 채팅 도메인 API/WebSocket 서버 (MongoDB, Redis, Kafka 연동)

### 모듈별 실행

- **User 서버**
  - 메인 클래스: `com.project.blinddate.user.UserServerApplication`
  - 기본 포트: `8081`
  - 헬스 체크: `GET /api/v1/users/health`

- **Chat 서버**
  - 메인 클래스: `com.project.blinddate.chat.ChatServerApplication`
  - 기본 포트: `8082`
  - 헬스 체크: `GET /api/v1/chats/health`

Gradle 멀티 모듈 구조이므로 루트에서 `./gradlew :user-server:bootRun`, `./gradlew :chat-server:bootRun` 으로 각각 실행합니다.
또는  `docker compose up -d --build user-server chat-server` 를 통해 docker 바로 실행

