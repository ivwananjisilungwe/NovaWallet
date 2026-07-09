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

### Phase 1 — Complete ✅
- ✅ User registration & JWT authentication (access + refresh token rotation)
- ✅ Secure PIN management with BCrypt + 3-attempt lockout (15-min cooldown)
- ✅ Email verification (token-based)
- ✅ Password reset flow
- ✅ Digital wallet creation with auto-generated account numbers (NW prefix)
- ✅ User profile management (get/update)
- ✅ Admin user management (list, get, soft-delete/restore)
- ✅ MDC request tracing (X-Request-ID)
- ✅ Comprehensive error handling with uniform JSON responses (7 exception types)
- ✅ Swagger/OpenAPI documentation
- ✅ H2 in-memory database for integration tests

### Phase 2+ — Planned
- ⏳ Deposits, withdrawals, and transfers
- ⏳ Transaction history with pagination and filtering
- ⏳ Configurable fee engine
- ⏳ Idempotency for financial operations
- ⏳ Rate limiting
- ⏳ Async email & SMS notifications
- ⏳ Audit logging for all balance changes

---

## Project Structure

```
novawallet-api/
├── src/main/java/com/novawallet/novawallet_api/
│   ├── admin/          # Admin user management (controller, service, dto)
│   ├── auth/           # Authentication (controller, service, dto/request, dto/response)
│   ├── common/         # Shared DTOs (ApiResponse)
│   ├── config/         # Security, OpenAPI, password encoding, JPA auditing
│   ├── exception/      # Global error handling (7 exception types)
│   ├── filter/         # Request tracing (X-Request-ID)
│   ├── security/       # JWT filter, JWT util, custom user details
│   ├── token/          # Refresh token entity, repository, service
│   ├── user/           # User management (controller, service, entity, dto)
│   └── wallet/         # Wallet management (controller, service, entity, dto)
├── src/main/resources/
│   ├── application.yml          # Base configuration
│   ├── application-dev.yml      # Development profile (PostgreSQL)
│   ├── application-prod.yml     # Production profile
│   └── db/migration/            # Flyway SQL migrations (V1-V4)
├── src/test/
│   ├── java/.../auth/           # Auth unit & integration tests
│   ├── java/.../config/         # Test configuration
│   └── resources/               # H2 test database config
└── docs/
    ├── report.md                # Project report
    └── testing-guide.md         # API testing guide with credentials
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

curl http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"TestPass123"}'
# {"success":true,"data":{"accessToken":"...","refreshToken":"...",...}}
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
| **1** | User & Authentication Core (register, login, JWT, PIN, email verify, password reset, profile, wallet, admin) | ✅ Complete |
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
