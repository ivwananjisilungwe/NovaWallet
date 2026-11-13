# =============================================================================
# NovaWallet API — Production-style multi-stage Dockerfile (D2)
#
# Stage 1: Build the executable Spring Boot JAR with Maven + JDK 17 in a
#          clean, isolated environment (independent of any host Maven cache).
# Stage 2: Run it on a minimal JRE 17 image as a dedicated non-root user.
# =============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy the dependency manifest first to leverage Docker layer caching.
COPY pom.xml .

# Resolve project dependencies only (compile + runtime, transitive).
# Deliberately NOT `dependency:go-offline`: that goal also resolves Maven's
# own site/reporting plugin tree (doxia, flexmark, ...) which NovaWallet does
# not use and which costs ~10x the download time. `dependency:resolve` is
# sufficient for reproducible packaging; `mvn package` fetches any plugin
# still missing on first run. This layer is cached unless pom.xml changes.
RUN mvn -B -DskipTests dependency:resolve

# Copy application source (after dependencies for better cache reuse).
COPY src ./src

# Package the executable Spring Boot JAR.
# Tests are executed separately in the regression-verification phase (Phase H);
# this build verifies clean packaging in an isolated environment. Skipping tests
# here keeps the image build focused on reproducible packaging; the full suite
# (144 tests) is run against the host build in Phase H.
RUN mvn -B package -DskipTests

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Dedicated non-root user for the application process.
RUN addgroup -S novawallet && adduser -S novawallet -G novawallet

WORKDIR /app

# Copy only the executable JAR produced by the builder stage.
COPY --from=builder /build/target/novawallet-api-*.jar /app/app.jar

# Prepare the KYC upload directory and log directory; hand ownership to the app user.
RUN mkdir -p /app/uploads/kyc /var/log/novawallet \
    && chown -R novawallet:novawallet /app/uploads /var/log/novawallet

# Run as the non-root user.
USER novawallet

# JVM / container-aware settings (not hard-coded resource limits).
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
# Production profile (override via environment if needed).
ENV SPRING_PROFILES_ACTIVE="prod"

# Application port.
EXPOSE 8080

# Healthcheck against the verified Actuator endpoint (permitAll, no auth).
# start-period gives Flyway migrations + startup time to complete.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Exec form via sh so JAVA_OPTS is expanded; exec replaces the shell so the
# JVM receives termination signals (SIGTERM) directly.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
