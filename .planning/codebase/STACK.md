# Technology Stack Overview

## Backend
- **Language:** Java 17
- **Framework:** Spring Boot 3.5.3
- **Build Tool:** Maven (wrapper `mvnw`)
- **Database:** PostgreSQL 16 (Flyway for migrations)
- **Security:** Spring Security with JWT authentication
- **Caching:** Caffeine
- **Documentation:** springdoc OpenAPI 2.8.9
- **Logging:** Logstash Logback encoder
- **Testing:** JUnit 5, SpringBootTest, H2 in‑memory DB for integration tests

## Frontend (Mobile App)
- **Framework:** Flutter (Dart 3.12.2)
- **State Management:** Riverpod (`flutter_riverpod`)
- **Routing:** go_router
- **HTTP Client:** Dio
- **Secure Storage:** flutter_secure_storage
- **Fonts:** google_fonts
- **Internationalization:** intl

## DevOps / CI
- **Containerisation:** Docker multi‑stage builds
- **Orchestration:** Docker Compose for local dev
- **CI:** GitHub Actions workflow (`.github/workflows/ci.yml`) runs Maven tests and builds Docker image.

## Additional Tools
- **Code Quality:** SpotBugs, Checkstyle via Maven plugins (not explicitly listed but common)
- **Metrics:** Spring Actuator
- **Dependency Management:** Maven Central, Dependabot (recommended to add)