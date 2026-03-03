# ==================================================
# Stage 1: Builder - Gradle 빌드
# ==================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle wrapper + 설정 파일 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# 모듈 build.gradle.kts 복사
COPY module-core/build.gradle.kts module-core/
COPY module-infrastructure/build.gradle.kts module-infrastructure/
COPY module-batch/build.gradle.kts module-batch/
COPY module-api/build.gradle.kts module-api/

# 의존성 다운로드 (캐시 레이어)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --continue || true

# 소스 코드 복사
COPY module-core/src module-core/src
COPY module-infrastructure/src module-infrastructure/src
COPY module-batch/src module-batch/src
COPY module-api/src module-api/src

# bootJar 빌드 (테스트 제외)
RUN ./gradlew :module-api:bootJar :module-batch:bootJar -x test --no-daemon

# plain jar 제거 (bootJar만 남김)
RUN rm -f module-api/build/libs/*-plain.jar module-batch/build/libs/*-plain.jar

# ==================================================
# Stage 2: API 이미지
# ==================================================
FROM eclipse-temurin:21-jre-alpine AS api

LABEL org.opencontainers.image.source="https://github.com/Girlee94/ready-japan"
LABEL org.opencontainers.image.description="Ready Japan API Service"

RUN apk add --no-cache curl ca-certificates && \
    addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/module-api/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# ==================================================
# Stage 3: Batch 이미지
# ==================================================
FROM eclipse-temurin:21-jre-alpine AS batch

LABEL org.opencontainers.image.source="https://github.com/Girlee94/ready-japan"
LABEL org.opencontainers.image.description="Ready Japan Batch Service"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/module-batch/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["java", "-jar", "app.jar"]
