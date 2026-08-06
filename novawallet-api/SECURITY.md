# NovaWallet Security Documentation

> **Version**: Phase 1 — Dev/Prototype  
> **Last updated**: 2026-07-10  
> **Status**: Functional security implemented; production hardening deferred

---

## Table of Contents

1. [Security Architecture Overview](#1-security-architecture-overview)
2. [Authentication Flow](#2-authentication-flow)
3. [Email Verification Flow](#3-email-verification-flow)
4. [Password Management](#4-password-management)
5. [PIN Management](#5-pin-management)
6. [Rate Limiting](#6-rate-limiting)
7. [Request Processing Pipeline](#7-request-processing-pipeline)
8. [Data Protection](#8-data-protection)
9. [Security Headers](#9-security-headers)
10. [Configuration Reference](#10-configuration-reference)
11. [Production Gaps](#11-production-gaps)

---

## 1. Security Architecture Overview

```
┌──────────────────────┐
│     Client App       │  (React Native / Flutter / Postman)
├──────────────────────┤
│   HTTP (no HTTPS)    │  ← Dev only. HTTPS required for production.
├──────────────────────┤
│  RateLimitFilter     │  ← Token-bucket: 100 req/min global, 10 req/min auth
│  (auto-registered)   │
├──────────────────────┤
│  Spring Security     │
│  Filter Chain        │  ← CSRF disabled, stateless sessions
├──────────────────────┤
│  JwtAuthFilter       │  ← Validates Bearer token on every authenticated request
├──────────────────────┤
│  Controller Layer    │  ← LoginRateLimiter wraps /login (5 failed → 15min lockout)
├──────────────────────┤
│  AuthService         │  ← BCrypt hashing, token generation, email triggers
├──────────────────────┤
│  PostgreSQL          │  ← Hashed passwords, hashed PINs, hashed refresh tokens
└──────────────────────┘
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Stateless with JWT** | No server-side session storage. Access tokens are self-contained JWTs. |
| **Refresh token rotation** | Every refresh invalidates the old token and issues a new one. Limits stolen-token window. |
| **Rate limiting at filter level** | Block abusive traffic before it reaches controllers or the database. |
| **Login lockout in controller** | Login-specific lockout (5 attempts) gives clearer user feedback than generic rate limiting. |
| **Async email** | Email sending is `@Async` — the register/forgot endpoint returns immediately without waiting for SMTP. |

---

## 2. Authentication Flow

### 2.1 Registration

```
POST /api/v1/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phone": "+260971234567",
  "password": "SecurePass@123"
}
```

**Server-side processing:**

1. **Validate input** — `@Valid` triggers Jakarta Bean Validation:
   - `firstName` / `lastName`: required, 2-50 chars
   - `email`: valid email format
   - `phone`: matches `^\+?[1-9]\d{6,14}$`
   - `password`: min 8 chars, must contain uppercase + lowercase + digit + special character
2. **Check duplicates** — `userRepository.existsByEmail()` and `existsByPhone()`. Returns `409 DuplicateResourceException` if taken.
3. **Hash password** — `BCryptPasswordEncoder.encode()` (strength 10 by default).
4. **Create user** — saves with `Role.USER`, `emailVerified=false`, generates UUID `verificationToken`. (No wallet yet — wallet is created after KYC approval in Phase 1.5.)
5. **Generate tokens** — JWT access token (15 min) + refresh token (7 days, hashed in DB).
6. **Send verification email** — `MailService.sendVerificationEmail()` runs asynchronously.
8. **Return** HTTP `201` with `AuthResponse`.

```
Response 201:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "expiresIn": 900000,
    "user": {
      "id": "uuid",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com",
      "emailVerified": false
    }
  },
  "message": "Registration successful"
}
```

### 2.2 Login

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass@123"
}
```

**Rate limiting step** (before credential check):
1. `LoginRateLimiter.isLocked(email, ip)` checks Caffeine cache keyed by `email:ip`.
2. If failure count ≥ 5 within 15-minute window → throws `RateLimitException` (HTTP 429).

**Credential check:**
1. `userRepository.findByEmail()` — returns 401 if not found.
2. `BCryptPasswordEncoder.matches(rawPassword, hashed)` — returns 401 if mismatch.
3. On failure → `LoginRateLimiter.recordFailedAttempt()` increments counter.
4. On success → `LoginRateLimiter.recordSuccessfulAttempt()` resets counter.

**Token generation:**
1. `JwtUtil.generateToken(userId, email, role)` creates JWT with claims `sub=userId`, `email`, `role`.
2. `TokenService.createRefreshToken()` stores hashed token in DB with expiry.
3. Returns same `AuthResponse` format as registration.

### 2.3 Token Refresh

```
POST /api/v1/auth/refresh
Authorization: Bearer <refreshToken>
```

1. Extract token from `Authorization: Bearer <value>` header.
2. Look up hash in `refresh_tokens` table.
3. Validate not expired.
4. **Rotate**: delete old token, issue new access + refresh token pair.
5. If a consumed (already-used) token is presented → **revoke all** tokens for that user (stolen token detection).

### 2.4 Token Structure

**Access Token (JWT):**
```json
{
  "sub": "user-uuid",
  "email": "john@example.com",
  "role": "USER",
  "iat": 1749571200,
  "exp": 1749572100
}
```
- Signed with HMAC-SHA256 using `app.jwt.secret`.
- Expires in 15 minutes (`app.jwt.expiration-ms`).

**Refresh Token:**
- UUID v4 stored as BCrypt hash in `refresh_tokens` table.
- Expires in 7 days.
- Single-use (rotated on every refresh).

---

## 3. Email Verification Flow

### 3.1 On Registration

`AuthService.register()` generates `verificationToken` (UUID) and saves it on the `User` entity.

`MailService.sendVerificationEmail()` is called **asynchronously** (`@Async`):
```java
@Async
public void sendVerificationEmail(String to, String token) {
    String link = "https://novawallet.com/verify-email?token=" + token;
    // Dev: log to console
    // Production: send via JavaMailSender
}
```

### 3.2 Verification

```
POST /api/v1/email/verify?token=<verificationToken>
```

1. `userRepository.findByVerificationToken(token)` — returns 400 if not found.
2. `user.emailVerified == true` — returns 400 "already verified".
3. Sets `emailVerified=true`, clears `verificationToken`.
4. Returns HTTP 200.

### 3.3 Dev Mode Behavior

Without SMTP configured, `MailService` **logs all emails to the console**:
```
[MailService] 📧 Verification email to john@example.com
  Link: https://novawallet.com/verify-email?token=abc-123-def
```

To extract tokens in dev:
- Check application logs for the link.
- Copy the token from the URL.
- Use Swagger UI or curl to POST it.

---

## 4. Password Management

### 4.1 Password Strength Requirements

Enforced by `RegisterRequest` validation annotations:

```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
    message = "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
)
```

| Requirement | Example |
|---|---|
| Minimum 8 characters | `abcD1!e` ❌ (7 chars) |
| At least one uppercase | `abcdefg1!` ❌ |
| At least one lowercase | `ABCDEFG1!` ❌ |
| At least one digit | `Abcdefgh!` ❌ |
| At least one special character | `Abcdefgh1` ❌ |
| Valid | `SecurePass@123` ✅ |

### 4.2 Forgot Password

```
POST /api/v1/password/forgot
{
  "email": "john@example.com"
}
```

1. Look up user by email.
2. **If found**: generate UUID `passwordResetToken`, set `passwordResetExpiresAt = now + 1 hour`.
3. **Always returns 200** (prevents email enumeration attacks — attacker can't tell if email exists).
4. Send reset email asynchronously with link: `https://novawallet.com/reset-password?token=<uuid>`.

### 4.3 Reset Password

```
POST /api/v1/password/reset
{
  "token": "reset-uuid",
  "newPassword": "NewSecure@456"
}
```

1. `userRepository.findByPasswordResetToken(token)` — 400 if not found.
2. Check `passwordResetExpiresAt > now` — 400 if expired.
3. Hash new password, save to user.
4. Clear reset token and expiry.
5. **Revoke all refresh tokens** for this user (forces re-login on all devices).
6. Returns HTTP 200.

---

## 5. PIN Management

### 5.1 Set PIN

```
POST /api/v1/pin
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "pin": "1234"
}
```

1. Requires valid JWT (authenticated user).
2. Validates PIN length (4-6 digits, numeric only).
3. Hashes PIN with BCrypt.
4. Resets `pinAttempts` to 0.
5. Saves to user record.

### 5.2 PIN Lockout

- Tracks failed PIN attempts via `User.pinAttempts` field.
- If `pinLockedUntil` is set and in the future → PIN operations are blocked.
- (Future: unlock via timeout or email verification.)

---

## 6. Rate Limiting

NovaWallet has **two independent rate-limiting systems** that work at different layers.

### 6.1 Global Rate Limiting (RateLimitFilter)

| Property | Layer | Scope |
|---|---|---|
| **Class** | `RateLimitFilter` | Servlet filter (auto-registered `@Component`) |
| **Algorithm** | Token-bucket (Caffeine cache) | Per key |
| **Default limit** | 100 requests / 60s window | All endpoints |
| **Auth endpoint limit** | 10 requests / 60s window | `/v1/auth/*`, `/v1/password/*`, `/v1/pin/*`, `/v1/email/*` |
| **Key** | `user:<userId>` if authenticated, `ip:<remoteAddr>` if anonymous | Per user/IP |
| **Cache max** | 50,000 entries | Global |
| **Response** | HTTP 429 + JSON body | On exceeded |

**Behavior:**
- Applied **before** Spring Security filter chain.
- Sets response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.
- On violation: returns 429 with `Retry-After` header.

**Configuration** (`application.yml`):
```yaml
app:
  rate-limit:
    global:
      max-requests: 100      # Default requests per window per user/IP
      auth-max-requests: 10  # Stricter limit for auth endpoints
      window-seconds: 60     # Rolling window duration
```

### 6.2 Login Rate Limiter (LoginRateLimiter)

| Property | Layer | Scope |
|---|---|---|
| **Class** | `LoginRateLimiter` | Controller-level (called by `AuthController.login()`) |
| **Algorithm** | Fixed counter with TTL (Caffeine cache) | Per email+IP |
| **Limit** | 5 failed attempts per window | Login only |
| **Window** | 15 minutes | From first failed attempt |
| **Key** | `<email>:<ip>` | Compound key |
| **Response** | HTTP 429 + JSON body | On exceeded |

**Behavior:**
- Only applies to `POST /v1/auth/login`.
- Counter resets on successful login.
- Locked accounts get `429 Account temporarily locked` message.
- Separate from global rate limiting — login can be locked independently.

### 6.3 Rate Limit Response Format

```json
HTTP 429 Too Many Requests
Retry-After: 60

{
  "success": false,
  "message": "Too many requests. Please try again later.",
  "code": "RATE_LIMITED"
}
```

---

## 7. Request Processing Pipeline

Every request to NovaWallet goes through this exact pipeline:

```
1. Tomcat / Servlet Container
        ↓
2. RateLimitFilter (@Component)
   - Resolve key (user ID if authenticated, IP if anonymous)
   - Increment counter, check against limit
   - Set X-RateLimit-* headers
   - If exceeded: return 429 immediately
        ↓
3. Spring Security Filter Chain
   - SecurityContextHolderFilter
   - DisableEncodeUrlFilter
   - SecurityContextHolderAwareRequestFilter  
   - AnonymousAuthenticationFilter
   - SessionManagementFilter (stateless — no session created)
   - ExceptionTranslationFilter
        ↓
4. FilterSecurityInterceptor
   - Check if path is permitted (public endpoints bypass auth)
   - If authenticated required → extract Bearer token via JwtAuthFilter
        ↓
5. JwtAuthFilter (OncePerRequestFilter, before UsernamePasswordAuthenticationFilter)
   - Extract Authorization header
   - Validate JWT signature + expiry
   - Set SecurityContext with userId, email, role
   - If invalid/missing: chain continues (Spring Security handles 401)
        ↓
6. Controller
   - AuthController.login() additionally checks LoginRateLimiter
   - Method called with validated @RequestBody DTO
        ↓
7. Service Layer
   - AuthService orchestrates business logic
   - Uses repositories, token service, mail service
        ↓
8. Exception Handling (if thrown)
   - GlobalExceptionHandler catches all exceptions
   - Returns consistent JSON error response
```

### Public vs Authenticated Endpoints

| Path | Auth Required | Rate Limited | Notes |
|---|---|---|---|
| `POST /v1/auth/register` | No | 10/min | +1 per registration |
| `POST /v1/auth/login` | No | 10/min + 5-attempt lockout | Dual rate limiting |
| `POST /v1/auth/refresh` | No | 10/min | Token in Authorization header |
| `POST /v1/password/forgot` | No | 10/min | Always returns 200 |
| `POST /v1/password/reset` | No | 10/min | |
| `POST /v1/email/verify` | No | 10/min | Query param token |
| `POST /v1/pin` | Yes | 10/min | |
| `GET /api-docs` | No | 100/min | Swagger OpenAPI spec |
| `GET /swagger-ui/**` | No | 100/min | Swagger UI |
| `GET /actuator/health` | No | 100/min | Health check |
| All other endpoints | Yes | 100/min | Requires valid JWT |

---

## 8. Data Protection

### 8.1 Fields Stored as Hashes (Never Plaintext)

| Field | Algorithm | Location |
|---|---|---|
| Password | BCrypt | `users.password_hash` |
| PIN | BCrypt | `users.pin_hash` |
| Refresh token | BCrypt | `refresh_tokens.token_hash` |

### 8.2 What We DON'T Store

- ❌ Plaintext passwords
- ❌ Plaintext PINs
- ❌ Plaintext refresh tokens
- ❌ Credit/debit card numbers (future: Flutterwave tokenizes)
- ❌ Government ID numbers (future: encrypted at rest)

### 8.3 JWT Signing

- Algorithm: HMAC-SHA256
- Secret: configured via `app.jwt.secret` (currently hardcoded in `application.yml`)
- Dev default: `"404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"` (64-char hex string)

---

## 9. Security Headers

Set by `SecurityConfig` for every response:

| Header | Value | Purpose |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | Prevents MIME-type sniffing |
| `X-Frame-Options` | `SAMEORIGIN` | Prevents clickjacking |
| `Cache-Control` | `no-cache, no-store, max-age=0, must-revalidate` | Prevents caching of sensitive data |
| `Pragma` | `no-cache` | HTTP/1.0 cache prevention |
| `Expires` | `0` | Cache expiration |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Forces HTTPS (once enabled) |
| `X-RateLimit-Limit` | Varies | Current rate limit for the key |
| `X-RateLimit-Remaining` | Varies | Remaining requests in window |
| `X-RateLimit-Reset` | Varies | Window duration in seconds |

---

## 10. Configuration Reference

All security-related configuration from `src/main/resources/application.yml`:

```yaml
app:
  jwt:
    secret: "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    expiration-ms: 900000  # 15 minutes

  mail:
    from: noreply@novawallet.com

  rate-limit:
    global:
      max-requests: 100       # Default: 100 requests per window
      auth-max-requests: 10   # Auth endpoints: 10 per window
      window-seconds: 60      # 1-minute rolling window

spring:
  mail:
    host: smtp.gmail.com      # Configure for production
    port: 587
    username:                 # Set via env var
    password:                 # Set via env var
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

## 11. Production Gaps

These are deferred until pre-launch hardening:

| # | Gap | Impact | Effort to Fix |
|---|---|---|---|
| 1 | **HTTPS** | Passwords travel in plaintext over the network | Low (Let's Encrypt) |
| 2 | **JWT secret in env var** | Secret hardcoded in repo | Low (one-line change) |
| 3 | **CORS** | No frontend domain restriction | Low (one config block) |
| 4 | **SMTP config** | Emails only log to console | Medium (SMTP credentials) |
| 5 | **CSRF protection** | Disabled (OK for mobile, risky for browser) | Low (re-enable with token) |
| 6 | **Refresh token cleanup** | No scheduled purge of expired tokens | Low (scheduled task) |
| 7 | **Audit logging** | No record of who did what | Medium (AOP aspect) |
| 8 | **Account freeze API** | Can't lock compromised accounts | Low (admin endpoint) |
| 9 | **Rate limit persistence** | Counters reset on server restart | Low (optional Redis) |
| 10 | **Secrets scanning** | `.env` / vault integration | Medium |

---

## Security Checklist — Launch Readiness

- [ ] HTTPS enabled (cert + redirect)
- [ ] JWT secret in environment variable (not repo)
- [ ] CORS whitelist configured
- [ ] SMTP credentials set (real email delivery)
- [ ] Rate limits tuned for expected traffic
- [ ] Refresh token cleanup scheduled (cron/quartz)
- [ ] Admin freeze/unfreeze API built
- [ ] Rate limit persistence (optional: Redis)
- [ ] Audit logging active
- [ ] Penetration test / security review completed
- [ ] `application.yml` secrets removed from version control
