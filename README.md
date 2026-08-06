# NovaWallet

A digital wallet platform for university students and small businesses in Zambia. Full-stack: Spring Boot REST API + Flutter mobile app.

## Architecture

```
NovaWallet/
├── novawallet-api/          # Spring Boot 3.5.3 / Java 17 REST API
│   ├── src/main/java/       # Production code
│   ├── src/main/resources/  # application.yml, Flyway migrations
│   ├── src/test/java/       # 28 test classes, 125 tests
│   └── pom.xml
└── novawallet-app/          # Flutter 3.x mobile app
    ├── lib/
    │   ├── core/            # Network, storage, theme, widgets
    │   ├── features/        # auth, wallet, transaction, kyc, admin, profile
    │   ├── models/          # Shared data models
    │   └── app/             # Router, providers
    └── test/                # 112 tests (repositories, providers, models, router)
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.5.3, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL, Flyway migrations |
| **Auth** | JWT (jjwt), refresh tokens, PIN-based transfers |
| **Mobile** | Flutter 3.x, Dart, Riverpod 2.6, go_router |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Infra** | Docker, GitHub Actions CI |

## Features

- **Auth**: Register, login, email verification, password reset, JWT + refresh token rotation
- **Wallets**: Create wallets, view balance, account numbers
- **Transactions**: Deposit, withdraw, transfer (PIN-protected), paginated history, fee estimation
- **KYC**: Document upload (image_picker), tiered verification, admin approval/rejection
- **Admin Dashboard**: User management, KYC queue, wallet freeze/unfreeze, fee configuration, audit logs
- **Notifications**: Email + SMS delivery with retry scheduling
- **Idempotency**: Request deduplication for financial operations
- **Security**: Rate limiting, CORS, HSTS, CSP, pessimistic locking on transfers

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+ (or use `./mvnw`)
- PostgreSQL 15+
- Flutter 3.x SDK

### Backend

```bash
cd novawallet-api

# Start PostgreSQL (or use docker-compose)
docker-compose up -d postgres

# Run tests
./mvnw test

# Start dev server (H2 in-memory DB)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API available at `http://localhost:8080/api/v1`
Swagger UI at `http://localhost:8080/swagger-ui.html`

### Mobile App

```bash
cd novawallet-app

# Install dependencies
flutter pub get

# Run tests
flutter test

# Run on device/emulator
flutter run
```

## Testing

### Backend (125 tests)

```bash
cd novawallet-api
./mvnw test
```

Covers: auth flows, transaction lifecycle, idempotency, rate limiting, fee engine, admin security, concurrent transfers, KYC endpoints.

### Flutter (112 tests)

```bash
cd novawallet-app
flutter test
```

Covers: API client (envelope unwrap, error normalization, refresh single-flight), all repositories, model serialization, auth state machine, router guards.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register new account |
| POST | `/api/v1/auth/login` | Login (returns JWT + refresh) |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/change-password` | Change password |
| POST | `/api/v1/pin/set` | Set transaction PIN |
| GET | `/api/v1/wallets/me` | Get user wallets |
| POST | `/api/v1/wallets/{id}/deposit` | Deposit funds |
| POST | `/api/v1/wallets/{id}/withdraw` | Withdraw funds |
| POST | `/api/v1/transfers` | Transfer funds (PIN required) |
| GET | `/api/v1/wallets/{id}/transactions` | Paginated transaction history |
| GET | `/api/v1/transactions/{reference}` | Transaction detail |
| GET | `/api/v1/fees/estimate` | Estimate fees |
| POST | `/api/v1/kyc/documents` | Upload KYC document |
| GET | `/api/v1/kyc/status` | Check KYC status |
| GET | `/api/v1/admin/kyc/pending` | Admin: pending KYC queue |
| POST | `/api/v1/admin/kyc/{id}/approve` | Admin: approve KYC |
| POST | `/api/v1/admin/kyc/{id}/reject` | Admin: reject KYC |
| GET | `/api/v1/admin/users` | Admin: user management |
| PUT | `/api/v1/admin/wallets/{id}/freeze` | Admin: freeze wallet |
| PUT | `/api/v1/admin/wallets/{id}/unfreeze` | Admin: unfreeze wallet |

## License

Private project.
