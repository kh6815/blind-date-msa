FROM gradle:8.10.0-jdk17-alpine AS builder

WORKDIR /workspace

# Gradle 캐시를 최대한 활용하기 위해 설정 파일 먼저 복사
COPY settings.gradle build.gradle gradlew /workspace/
COPY gradle /workspace/gradle

# 멀티 모듈 소스 복사
COPY user-server /workspace/user-server
COPY chat-server /workspace/chat-server

# 필요한 모듈만 빌드
RUN ./gradlew :user-server:bootJar --no-daemon

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

COPY --from=builder /workspace/user-server/build/libs/user-server-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]


