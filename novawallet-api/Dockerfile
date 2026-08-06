# =============================================================================
# NovaWallet API — Multi-Stage Dockerfile
#
# Stage 1: Build with Maven + JDK 17
# Stage 2: Run with JRE 17 (slim alpine image)
# =============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies first (leverages Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -S novawallet && adduser -S novawallet -G novawallet

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create upload directory for KYC documents
RUN mkdir -p /app/uploads/kyc && chown -R novawallet:novawallet /app/uploads

# Switch to non-root user
USER novawallet

# Health check — Spring Boot actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

# Port
EXPOSE 8080

# Entry point
ENTRYPOINT ["java", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]
