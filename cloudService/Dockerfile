FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

COPY --from=builder /app/target/cloudService-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/files /app/logs && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    FILE_STORAGE_PATH=/app/files

ENTRYPOINT ["java", "-jar", "app.jar"]