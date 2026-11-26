# ========================================================
# FoodShare Production Multi-Stage Dockerfile
# Base: Eclipse Temurin Java 21 Alpine
# Security: Non-Root Execution User, Least-Privilege Architecture
# ========================================================

FROM eclipse-temurin:21-jre-alpine

# Set deployment metadata
LABEL maintainer="FoodShare Engineering Team"
LABEL version="1.0.0"

# Install curl for container health checks
RUN apk add --no-cache curl tzdata

# Create dedicated unprivileged system group and user
RUN addgroup -S foodshare && adduser -S foodshare -G foodshare

WORKDIR /app

# Copy the pre-built application jar
COPY target/foodshare-1.0.0.jar app.jar

# Enforce file ownership for non-root runtime
RUN chown -R foodshare:foodshare /app

# Switch to non-root user
USER foodshare

# Dynamic cloud port support (default 8080)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Container liveness health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:${PORT}/actuator/health/liveness || exit 1

# Execute Spring Boot application
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dserver.port=${PORT}", \
  "-jar", "app.jar"]
