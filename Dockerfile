# syntax=docker/dockerfile:1

# ---- Stage 1: Build with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache Maven dependencies across builds
COPY pom.xml .
# Copy source and build the fat JAR
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -Dmaven.test.skip=true -B

# ---- Stage 2: Run with JRE ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the built JAR from the build stage
COPY --from=build /build/target/*.jar app.jar

# One-off account creation helper (docker exec ... account-cli --create-admin)
COPY docker/account-cli.sh /usr/local/bin/account-cli
RUN chmod 755 /usr/local/bin/account-cli

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:${PORT:-8080}/api/buses || exit 1

ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-render} -Dserver.port=${PORT:-8080} -jar app.jar"]
