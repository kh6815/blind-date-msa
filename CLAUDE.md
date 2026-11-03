# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Blind Date is a real-time dating app backend built with **MSA (Microservices Architecture)** using Spring Boot 3.x, Java 17, and Gradle multi-module setup. The system separates User and Chat domains into independent services that communicate via Kafka events.

**Live URL:** https://user.blind-date.site
**Test Account:** user@test.com / 1234

## Architecture

### Services
- **user-server** (port 8081): User/Profile/Authentication domain with MySQL, JPA, Flyway
- **chat-server** (port 8082): Real-time chat with WebSocket (STOMP), MongoDB, Mongock

### Infrastructure
- **MySQL 8**: User server transactional data (users, user_images tables)
- **MongoDB 7**: Chat server document storage (chat_rooms, chat_messages collections)
- **Redis Cluster**: 6 nodes (3 master + 3 replica) for session/cache/Pub-Sub
- **Kafka Cluster**: 3 brokers + Zookeeper for event-driven communication between services
- **MinIO**: Object storage for profile/chat images
- **Nginx**: HTTPS termination, reverse proxy, load balancing across 2 user + 2 chat instances
- **Monitoring**: Prometheus + Grafana + Loki + Alloy for metrics and logs

### Inter-Service Communication
Services communicate **exclusively via Kafka** (not HTTP). Key topics:
- `chat-message-save`: Chat server consumes to persist messages asynchronously
- `user-info-updated`: Chat server consumes to invalidate user cache

## Development Commands

### Build & Run
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :user-server:bootJar
./gradlew :chat-server:bootJar

# Run user-server locally (requires dependencies: MySQL, Redis, Kafka, MinIO)
./gradlew :user-server:bootRun

# Run chat-server locally
./gradlew :chat-server:bootRun

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :user-server:test
./gradlew :chat-server:test

# Clean build
./gradlew clean
```

### Docker Environment
```bash
# Start all services (MySQL, MongoDB, Redis Cluster, Kafka Cluster, MinIO, app servers, monitoring)
docker-compose up -d

# Start production environment
docker-compose -f docker-compose-prod.yml up -d

# View logs
docker-compose logs -f user-server-1
docker-compose logs -f chat-server-1

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v
```

### Key Service Dependencies
- **user-server** startup requires: `mysql-user` → `redis-cluster-init` → `kafka-1` (healthy) → `minio`
- **chat-server** startup requires: `mongo-chat` → `redis-cluster-init` → `kafka-1` (healthy) → `minio`
- Both servers have health checks at `/api/v1/{users|chats}/health`

## Code Architecture Patterns

### Layer Structure
```
Controller → Service → Repository → Entity
           ↓
          DTO (MapStruct)
```

### Key Conventions (from .cursorrules)
1. **Unified Response Format**: All controllers return `ResponseDto` with snake_case JSON fields
2. **API Path Constants**: Use `ApiPathConst.USER_API_PREFIX` or `ApiPathConst.CHAT_API_PREFIX` in `@RequestMapping`
3. **Exception Handling**: Business exceptions wrapped in `CustomException`, handled by `@RestControllerAdvice`
4. **DTO Rules**: Request/Response DTOs use `@JsonNaming(SnakeCaseStrategy.class)`, `@Schema`, Bean Validation annotations
5. **Kafka Event Publishing**: User server publishes events; Chat server subscribes (see `ChatKafkaConsumer`, `UserInfoCacheService`)
6. **Transaction Management**: Domain-specific `@Transactional` with explicit transaction managers

### Technology Stack
- **Java 17**: Switch expressions, text blocks, records (for DTOs use `class` not `record`)
- **Lombok**: `@Getter`, `@Builder`, `@RequiredArgsConstructor` (NO `@Setter`)
- **MapStruct**: Entity ↔ DTO mapping
- **QueryDSL** (user-server): Type-safe complex queries
- **JWT**: Authentication tokens (user-server)
- **WebSocket STOMP** (chat-server): Real-time chat
- **P6Spy**: SQL logging
- **Logstash Logback Encoder**: JSON-formatted logs sent to Loki

### Testing
- **JUnit 5 + Mockito + AssertJ**
- Given-When-Then pattern
- Test method names can use Korean with `@DisplayName`
- Integration tests: `@SpringBootTest` + TestContainers (recommended)

## Module Structure

### user-server (`/user-server/src/main/java/com/project/blinddate/user`)
- `config/`: Security, Kafka, MinIO, QueryDSL, CORS, Redis
- `controller/`: REST API + Thymeleaf view controllers
- `domain/`: JPA entities (User, UserImage)
- `dto/`: Request/Response DTOs with snake_case JSON
- `exception/`: Global exception handlers
- `filter/`: Request logging filters
- `logger/`: Custom MDC logging, JSON layout
- `mapper/`: MapStruct mappers
- `repository/`: JPA + QueryDSL repositories
- `security/`: JWT token utilities
- `service/`: Business logic + Kafka producers
- `aop/`: Activity logging

### chat-server (`/chat-server/src/main/java/com/project/blinddate/chat`)
- `config/`: Security, MongoDB, Redis, Kafka, WebSocket, CORS
- `controller/`: REST API + WebSocket + Thymeleaf views
- `domain/`: MongoDB documents (ChatRoom, ChatMessage, MessageType)
- `dto/`: Request/Response DTOs
- `exception/`: Global exception handlers
- `external/`: Feign client for user-server (if needed, though prefer Kafka)
- `filter/`: Request logging
- `logger/`: MDC logging
- `mapper/`: MapStruct mappers
- `migration/`: Mongock migration scripts
- `repository/`: MongoDB repositories
- `service/`: Chat business logic + Kafka consumers + Redis Pub/Sub
- `aop/`: Chat activity logging + caching

### Database Migrations
- **user-server**: Flyway scripts in `src/main/resources/db/migration/` (versioned SQL)
- **chat-server**: Mongock change logs in `migration/` package (Java-based)

## Configuration Management

### Environment Variables (`.env`)
Key variables injected via docker-compose:
- `SPRING_DATASOURCE_*`: MySQL connection
- `SPRING_DATA_MONGODB_URI`: MongoDB connection
- `SPRING_DATA_REDIS_CLUSTER_NODES`: 6 Redis nodes
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: 3 Kafka brokers
- `MINIO_ENDPOINT`, `MINIO_PUBLIC_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `COOKIE_DOMAIN`
- `EXTERNAL_USER_SERVER_URL`, `EXTERNAL_CHAT_SERVER_URL`

### Profiles
- Default profile uses environment variables
- Docker profile: `SPRING_PROFILES_ACTIVE=docker` in docker-compose

## Monitoring & Observability

### Prometheus Metrics
- Actuator endpoints: `/actuator/prometheus`
- Config: `monitoring/prometheus/config/prometheus.yml`
- Data persisted in `monitoring/prometheus/data/`

### Logs (Loki + Alloy)
- Application logs written to `./logs/user-server-{1,2}/`, `./logs/chat-server-{1,2}/`
- Alloy agent tails logs and pushes to Loki
- Alloy config: `monitoring/alloy/config/config.alloy`
- Loki config: `monitoring/loki/config/loki.yml`

### Grafana Dashboards
- Accessible at port 4000
- Datasources: Prometheus + Loki
- Dashboards in `monitoring/grafana/setting/dashboards/`

## Important Implementation Notes

1. **No HTTP between services**: User and Chat servers communicate only via Kafka events
2. **Redis Cluster Mode**: Applications connect to all 6 nodes; cluster creation happens via `redis-cluster-init` service
3. **Kafka High Availability**: 3 brokers with `replication-factor=3`, `min-insync-replicas=2`
4. **Health Checks**: Docker services use health checks; apps start only after dependencies are healthy
5. **Horizontal Scaling**: Nginx load balances across 2 user-server and 2 chat-server instances
6. **HTTPS**: Nginx terminates SSL with certificates in `cert/` directory (mkcert for local dev)
7. **Network**: All services on `blind-date-app-net` bridge network; communicate via service names

## API Documentation

Swagger UI available when servers are running:
- User Server: http://localhost:8081/swagger-ui.html
- Chat Server: http://localhost:8082/swagger-ui.html

## Common Development Scenarios

### Adding a new Kafka event
1. Define topic constant in `application.yaml` under `chat.kafka.topics`
2. User server: Create producer in service layer, publish event
3. Chat server: Add `@KafkaListener` method in appropriate service
4. Update Kafka config beans if new serializers needed

### Adding a new REST endpoint
1. Define path constant in `ApiPathConst`
2. Create DTO with `@JsonNaming(SnakeCaseStrategy.class)`, `@Schema`, validation
3. Implement controller method with `@Operation`, `@ApiResponses`
4. Return `ResponseDto` wrapping the response
5. Service layer implements business logic
6. Repository layer for data access

### Database schema changes
- **User Server**: Create new Flyway migration `V{version}__{description}.sql` in `user-server/src/main/resources/db/migration/`
- **Chat Server**: Create Mongock change log class in `chat.migration` package

### Working with Redis cache
- User presence tracking: `user:presence:{userId}` keys
- Auth tokens: `user:token:{token}` keys
- Use Redis Cluster commands (lettuce client auto-handles redirects)

## Security Notes

- JWT secret must be strong (set via `JWT_SECRET` env var)
- Spring Security configured in both servers
- CORS configured for specific origins (update `CorsConfig` classes)
- Cookie domain must match deployment environment

## Future Enhancements (from README)

- Like feature and notifications
- Distributed locks with Redis for concurrency control
- CI/CD pipeline (GitHub Actions)
- Kubernetes deployment