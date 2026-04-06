# ============================================
# Build Stage
# ============================================
FROM gradle:8.10.0-jdk17-alpine AS builder

WORKDIR /workspace

# 1단계: Gradle 설정 파일만 먼저 복사 (캐싱 레이어)
COPY settings.gradle build.gradle gradlew /workspace/
COPY gradle /workspace/gradle

# 2단계: 빌드 스크립트만 복사 (의존성 다운로드용)
COPY user-server/build.gradle /workspace/user-server/
COPY chat-server/build.gradle /workspace/chat-server/

# 3단계: 의존성 다운로드 (소스 코드 변경과 무관하게 캐싱됨)
RUN ./gradlew :chat-server:dependencies --no-daemon || true

# 4단계: 전체 소스 복사
COPY user-server /workspace/user-server
COPY chat-server /workspace/chat-server

# 5단계: 빌드 (의존성은 이미 캐싱되어 있음)
RUN ./gradlew :chat-server:bootJar --no-daemon

# ============================================
# Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# mkcert root CA 복사
COPY cert/rootCA.pem /rootCA.pem

# JVM truststore에 mkcert CA 등록
RUN keytool -importcert \
    -alias mkcert-root \
    -file /rootCA.pem \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \
    -noprompt

# 빌드된 JAR 파일 복사
COPY --from=builder /workspace/chat-server/build/libs/chat-server-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]