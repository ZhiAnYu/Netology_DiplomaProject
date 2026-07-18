FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /app/target/cloudService-0.0.1-SNAPSHOT.jar app.jar

# COPY --from=builder /app/src/main/resources/application.yml ./config/

RUN mkdir -p /app/files /app/logs && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    DB_HOST=postgres \
    DB_PORT=5432 \
    DB_NAME=cloud_db \
    DB_USER=user \
    DB_PASSWORD=password \
    FILE_STORAGE_PATH=/app/files

ENTRYPOINT ["java", "-jar", "app.jar"]