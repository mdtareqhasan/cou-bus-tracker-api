# Remoud builds from the Git repository root. The Spring Boot project itself
# lives in Backend/, so this root-level Dockerfile keeps the PaaS build context
# correct without moving the existing local Docker setup.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY Backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY Backend/src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && apk add --no-cache curl

COPY --from=build /build/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

# Default to the Render/Railway profile so the app does not try to connect to a
# non-existent local PostgreSQL instance when no profile env var is provided.
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD ["sh", "-c", "curl -fsS http://localhost:${PORT:-8080}/api/buses || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-render} -Dserver.port=${PORT:-8080} -jar app.jar"]
