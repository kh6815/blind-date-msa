FROM gradle:8.10.0-jdk17-alpine AS builder

WORKDIR /workspace

COPY settings.gradle build.gradle gradlew /workspace/
COPY gradle /workspace/gradle

COPY user-server /workspace/user-server
COPY chat-server /workspace/chat-server

RUN ./gradlew :chat-server:bootJar --no-daemon

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

COPY --from=builder /workspace/chat-server/build/libs/chat-server-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]


