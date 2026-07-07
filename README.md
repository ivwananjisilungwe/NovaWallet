# NovaWallet API

A secure digital wallet backend API built with Spring Boot, enabling financial transactions, user management, and mobile money-style operations for university students and small businesses in Zambia.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.3 |
| **Database** | PostgreSQL 16 |
| **Migrations** | Flyway |
| **Security** | Spring Security + JWT (jjwt 0.12.x) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Cache** | Caffeine |
| **Build** | Maven |

---

## Features

- ✅ User registration & JWT authentication
- ✅ Digital wallet creation with auto-generated account numbers
- ✅ Deposits, withdrawals, and transfers with PIN verification
- ✅ Transaction history with pagination and filtering
- ✅ Configurable fee engine
- ✅ Idempotency for financial operations
- ✅ Rate limiting
- ✅ Async email & SMS notifications
- ✅ Admin endpoints for user/wallet management
- ✅ Audit logging for all balance changes
- ✅ MDC request tracing (X-Request-ID)
- ✅ Comprehensive error handling with uniform JSON responses

---

## Project Structure

```
novawallet-api/
├── src/main/java/com/novawallet/novawallet_api/
│   ├── audit/          # Audit logging (async)
│   ├── config/         # Security, OpenAPI, password encoding
│   ├── controller/     # REST endpoints
│   ├── dto/            # Request/response records
│   ├── entity/         # JPA entities
│   ├── exception/      # Global error handling
│   ├── filter/         # Request tracing, idempotency, rate limiting
│   ├── repository/     # Spring Data JPA repositories
│   ├── scheduler/      # Scheduled cleanup jobs
│   ├── security/       # JWT filter, user details
│   └── service/        # Business logic
├── src/main/resources/
│   ├── application.yml          # Base configuration
│   ├── application-dev.yml      # Development profile
│   ├── application-prod.yml     # Production profile
│   └── db/migration/            # Flyway SQL migrations
```

---

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL 16+
- Maven

### Setup

```bash
# Clone the repo
git clone https://github.com/ivwananjisilungwe/NovaWallet.git
cd NovaWallet/novawallet-api

# Create the database
createdb -U postgres novawallet

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verify it's running

```bash
curl http://localhost:8080/api/actuator/health
# {"status":"UP"}

curl http://localhost:8080/api/test/success
# {"success":true,"data":"NovaWallet API is running",...}
```

---

## API Documentation

Once running, visit:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

---

## Phase Progress

| Phase | Description | Status |
|---|---|---|
| **0** | Project Foundation | ✅ Complete |
| **1** | User & Authentication Core | 📝 In Progress |
| **2** | Transaction Core | ⏳ Planned |
| **3** | Transaction History & Querying | ⏳ Planned |
| **4** | Fee Engine & Audit Logging | ⏳ Planned |
| **5** | Refresh Token, Rate Limiting & Idempotency | ⏳ Planned |
| **6** | Notifications, Scheduling & Caching | ⏳ Planned |
| **7** | Admin Endpoints & Hardening | ⏳ Planned |
| **8** | Testing & QA | ⏳ Planned |

---

## Author

**Ivwananji Silungwe**

---

## License

This project is for educational and development purposes.
