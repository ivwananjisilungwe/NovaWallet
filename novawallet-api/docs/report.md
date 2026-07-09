# NovaWallet API — Project Report

**Generated**: 2026-07-09  
**Version**: 0.0.1-SNAPSHOT  
**Stack**: Java 17, Spring Boot 3.5.3, PostgreSQL 16, H2 (test)

---

## 1. Project Structure

```
src/main/java/com/novawallet/novawallet_api/
├── NovawalletApiApplication.java
├── admin/
│   ├── controller/AdminController.java
│   ├── dto/UserSummaryResponse.java
│   └── service/AdminService.java
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ForgotPasswordRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── ResetPasswordRequest.java
│   │   │   └── SetPinRequest.java
│   │   └── response/
│   │       └── AuthResponse.java
│   └── service/
│       └── AuthService.java
├── common/
│   └── dto/
│       └── ApiResponse.java
├── config/
│   ├── JpaAuditingConfig.java
│   ├── OpenApiConfig.java
│   ├── PasswordConfig.java
│   └── SecurityConfig.java
├── exception/
│   ├── BadRequestException.java
│   ├── DuplicateResourceException.java
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── filter/
│   └── RequestTracingFilter.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthFilter.java
│   └── JwtUtil.java
├── token/
│   ├── entity/RefreshToken.java
│   ├── repository/RefreshTokenRepository.java
│   └── service/TokenService.java
├── user/
│   ├── controller/UserController.java
│   ├── dto/
│   │   ├── request/UpdateProfileRequest.java
│   │   └── response/UserProfileResponse.java
│   ├── entity/
│   │   ├── Role.java
│   │   └── User.java
│   ├── repository/UserRepository.java
│   └── service/UserService.java
└── wallet/
    ├── controller/WalletController.java
    ├── dto/WalletResponse.java
    ├── entity/
    │   ├── FreezeReason.java
    │   ├── Wallet.java
    │   └── WalletStatus.java
    ├── repository/WalletRepository.java
    └── service/
        ├── AccountNumberGenerator.java
        └── WalletService.java
```

---

## 2. Database Migrations (Flyway)

| Migration | Description |
|-----------|-------------|
| V1__init_schema.sql | Schema version test table |
| V2__create_users_table.sql | Users table with soft-delete, PIN, email verification, password reset |
| V3__create_wallets_table.sql | Wallets table with account number, balance, freeze support |
| V4__create_refresh_tokens_table.sql | Refresh tokens table with token hash, expiry, revocation |

### Entity Model

```
User ──1:N──> RefreshToken
User ──1:1──> Wallet
```

---

## 3. API Endpoints

### Auth (`/v1/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /register | Public | Register new user + create wallet |
| POST | /login | Public | Login with email/password |
| POST | /refresh | Public | Refresh access token using refresh token |
| POST | /logout | JWT | Revoke all refresh tokens |
| POST | /pin | JWT | Set 4-6 digit PIN |
| POST | /pin/verify | JWT | Verify PIN (3 attempts before 15-min lockout) |
| GET | /verify?token=... | Public | Verify email address |
| POST | /forgot-password | Public | Request password reset |
| POST | /reset-password | Public | Reset password with token |

### User (`/v1/users`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /me | JWT | Get own profile |
| PUT | /me | JWT | Update name/phone |

### Wallet (`/v1/wallets`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /me | JWT | Get own wallet |

### Admin (`/v1/admin`) — Requires ROLE_ADMIN

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /users | JWT+Admin | List all users |
| GET | /users/{id} | JWT+Admin | Get user by ID |
| PATCH | /users/{id}/toggle-delete | JWT+Admin | Soft-delete / restore user |

### Actuator

| Path | Description |
|------|-------------|
| /actuator/health | Health check |
| /actuator/info | Application info |

---

## 4. Security Architecture

- **JWT** (HMAC-SHA384 via JJWT 0.12.7): Access tokens with 15-min TTL
- **Refresh Tokens**: UUID-based, SHA-256 hashed in DB, 7-day TTL, rotation on use
- **Password hashing**: BCrypt via Spring Security
- **PIN hashing**: BCrypt (4-6 digit, 3-attempt lockout, 15-min cooldown)
- **Session**: Stateless (no HTTP session)
- **Soft-delete**: `@SQLRestriction("deleted = false")` on User entity, with native query for admin restore

---

## 5. Test Results

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test Class | Type | Count | Status |
|------------|------|-------|--------|
| AuthServiceTest | Unit (mocked) | 17 | ✅ All pass |
| AuthControllerIntegrationTest | Integration (H2) | 4 | ✅ All pass |
| NovawalletApiApplicationTests | Context load | 1 | ✅ Pass |

---

## 6. Bugs Fixed

| # | Issue | Fix |
|---|-------|-----|
| 1 | `@SQLRestriction` prevented admin from restoring soft-deleted users | Added `findByIdIncludingDeleted()` native query in UserRepository |
| 2 | Dead `"/test/**"` permitAll in SecurityConfig after TestController deletion | Removed the stale path |
| 3 | Refresh endpoint silently accepted malformed Authorization headers | Added Bearer prefix validation + proper 401 response |

---

## 7. Starting the Application

```bash
# Prerequisites: PostgreSQL running on localhost:5432, database 'novawallet'
cd novawallet-api
./mvnw spring-boot:run
```

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs JSON**: http://localhost:8080/api/api-docs
- **Health**: http://localhost:8080/api/actuator/health
