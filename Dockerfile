# ---- Stage 1: Build with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Stage 2: Run with JRE ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the built JAR from the build stage
COPY --from=build /build/target/*.jar app.jar

# Uploaded images are stored in Cloudinary, not in the container filesystem.
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Default to the Render/Railway profile so the app does not try to connect to a
# non-existent local PostgreSQL instance when no profile env var is provided.
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:${PORT:-8080}/api/buses || exit 1

ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-render} -Dserver.port=${PORT:-8080} -jar app.jar"]
