# NovaWallet API — Comprehensive Testing Guide

> **Version**: Phase 1  
> **Base URL**: `http://localhost:8080/api`  
> **Swagger UI**: `http://localhost:8080/api/swagger-ui/index.html`  
> **OpenAPI JSON**: `http://localhost:8080/api/api-docs`

---

## Table of Contents

1. [Setup & Prerequisites](#1-setup--prerequisites)
2. [Error Response Format](#2-error-response-format)
3. [Authentication Endpoints](#3-authentication-endpoints)
   - 3.1 Register — `/v1/auth/register`
   - 3.2 Login — `/v1/auth/login`
   - 3.3 Refresh Token — `/v1/auth/refresh`
4. [Email Verification](#4-email-verification)
   - 4.1 Verify Email — `/v1/email/verify`
5. [Password Management](#5-password-management)
   - 5.1 Forgot Password — `/v1/password/forgot`
   - 5.2 Reset Password — `/v1/password/reset`
6. [PIN Management](#6-pin-management)
   - 6.1 Set PIN — `/v1/pin`
7. [User Profile](#7-user-profile)
   - 7.1 Get Profile — `GET /v1/users/me`
   - 7.2 Update Profile — `PUT /v1/users/me`
8. [Admin Endpoints](#8-admin-endpoints)
   - 8.1 List Users — `GET /v1/admin/users`
   - 8.2 Get User — `GET /v1/admin/users/{id}`
   - 8.3 Toggle Delete — `PATCH /v1/admin/users/{id}/toggle-delete`
9. [Rate Limiting Tests](#9-rate-limiting-tests)
10. [Edge Case Matrix](#10-edge-case-matrix)
11. [Quick Smoke Test Script](#11-quick-smoke-test-script)

---

## 1. Setup & Prerequisites

### Start the API

```bash
cd /media/ivwananji-silungwe/DEVELOPMENT/PROGRAMMING/PROJECTS/NovaWallet/novawallet-api

# Start the app
./mvnw spring-boot:run
```

### Verify it's running

```bash
curl -s http://localhost:8080/api/actuator/health
```

**Expected**: `{"status":"UP"}`

### Save as reusable variable

```bash
BASE=http://localhost:8080/api

# Check
echo $BASE
# Should print: http://localhost:8080/api
```

### Extract verification tokens from logs

In dev mode, emails are logged to console. Start the app in another terminal or check the log output:

```bash
# Watch for email tokens
tail -f /tmp/novawallet.log | grep -i "verification\|reset\|token"
```

Or run the app with logs visible and look for lines like:
```
[MailService] 📧 Verification email to john@example.com
  Link: https://novawallet.com/verify-email?token=abc-123-def-456
```

### Helper for parsing JSON responses

```bash
# Pretty-print any JSON response
alias pp='python3 -m json.tool'

# Extract a value from JSON
extract() {
  python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"
}

# Example:
# curl -s $BASE/v1/auth/login ... | extract data.accessToken
```

---

## 2. Error Response Format

All errors follow a consistent JSON format:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2026-07-10T18:44:19.871257247",
  "path": "/api/v1/auth/register",
  "errors": {
    "fieldName": "Error message for that field"
  }
}
```

### Error Codes Reference

| HTTP Status | `code` Field | Meaning |
|---|---|---|
| **400** | `VALIDATION_ERROR` | Input validation failed. `errors` object contains per-field messages. |
| **400** | `BAD_REQUEST` | Business logic error (invalid PIN, already verified, expired token). |
| **401** | `UNAUTHORIZED` | Bad credentials, invalid/expired JWT, missing Authorization header. |
| **403** | `FORBIDDEN` | Authenticated but not authorized (non-admin on admin endpoint). |
| **409** | `DUPLICATE_RESOURCE` | Email or phone already registered. |
| **429** | `RATE_LIMITED` | Too many requests. Check `Retry-After` header. |
| **500** | `INTERNAL_SERVER_ERROR` | Unexpected server error. Check application logs. |

---

## 3. Authentication Endpoints

### 3.1 Register — `POST /v1/auth/register`

**Request body:**
```json
{
  "firstName": "string (2-50 chars, required)",
  "lastName": "string (2-50 chars, required)",
  "email": "string (valid email, required)",
  "phone": "string (+ and 10-15 digits, required)",
  "password": "string (8+ chars, uppper+lower+digit+special, required)"
}
```

#### ✅ Success case

```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.test@example.com",
    "phone": "+260971234001",
    "password": "SecurePass@123"
  }' | python3 -m json.tool
```

**Expected**: HTTP `201 Created`
```json
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
      "email": "john.test@example.com",
      "role": "USER",
      "emailVerified": false,
      "pinSet": false
    }
  },
  "message": "Registration successful"
}
```

> ⚠️ **Note**: No wallet is created on registration. Wallet is created after KYC approval (Phase 1.5).

#### ❌ Edge Cases

**a) Duplicate email**
```bash
# Register with same email again
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.test@example.com",
    "phone": "+260971234002",
    "password": "SecurePass@123"
  }' | python3 -m json.tool
```
**Expected**: HTTP `409 Conflict`, code `DUPLICATE_RESOURCE`, message "Email already registered"

**b) Duplicate phone**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.test@example.com",
    "phone": "+260971234001",
    "password": "SecurePass@123"
  }' | python3 -m json.tool
```
**Expected**: HTTP `409 Conflict`, code `DUPLICATE_RESOURCE`, message "Phone number already registered"

**c) All fields empty**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "",
    "lastName": "",
    "email": "",
    "phone": "",
    "password": ""
  }' | python3 -m json.tool
```
**Expected**: HTTP `400 Bad Request`, code `VALIDATION_ERROR`, errors object shows messages for all 5 fields.

**d) Invalid email format**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "not-an-email",
    "phone": "+260971234003",
    "password": "SecurePass@123"
  }' | python3 -m json.tool
```
**Expected**: HTTP `400`, code `VALIDATION_ERROR`, errors.email = "Email must be valid"

**e) Weak password — no special char**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.weak1@example.com",
    "phone": "+260971234010",
    "password": "SecurePass123"
  }' | python3 -m json.tool
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`. Password must have uppercase + lowercase + digit + special character.

**f) Weak password — too short**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.weak2@example.com",
    "phone": "+260971234011",
    "password": "Ab1!"
  }' | python3 -m json.tool
```
**Expected**: HTTP `400`

**g) Invalid phone number**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.phone@example.com",
    "phone": "abc",
    "password": "SecurePass@123"
  }' | python3 -m json.tool
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`, errors.phone = "Phone number must be valid..."

**h) Missing body fields**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "only.email@example.com"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR` — missing firstName, lastName, phone, password.

**i) Empty JSON body**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR` — all required fields missing.

**j) Invalid JSON**
```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d 'not json at all'
```
**Expected**: HTTP `400` (Spring handles parse errors).

---

### 3.2 Login — `POST /v1/auth/login`

**Request body:**
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

#### ✅ Success case

```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.test@example.com",
    "password": "SecurePass@123"
  }' | python3 -m json.tool
```

**Expected**: HTTP `200 OK` — returns same `AuthResponse` format as register. Save the access token.

```bash
# Save tokens for subsequent tests
TOKEN=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"SecurePass@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

REFRESH=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"SecurePass@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['refreshToken'])")

echo "Token: ${TOKEN:0:40}..."
echo "Refresh: ${REFRESH:0:40}..."
```

#### ❌ Edge Cases

**a) Wrong password**
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.test@example.com",
    "password": "WrongPassword@456"
  }' | python3 -m json.tool
```
**Expected**: HTTP `401 Unauthorized`, code `UNAUTHORIZED`, message "Invalid email or password"

**b) Non-existent email**
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "does.not.exist@example.com",
    "password": "SomePass@123"
  }' | python3 -m json.tool
```
**Expected**: HTTP `401`, code `UNAUTHORIZED`, message "Invalid email or password"
> Same message as wrong password — prevents user enumeration.

**c) Empty email/password**
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"","password":""}'
```
**Expected**: HTTP `400`, code `VALIDATION_ERROR`

**d) Empty body**
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**e) Invalid JSON**
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{broken json}'
```
**Expected**: HTTP `400`

**f) Login lockout (5 failed attempts → 15 min block)**

Run this 5 times:
```bash
for i in $(seq 1 5); do
  echo "--- Attempt $i ---"
  curl -s -X POST $BASE/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "email": "john.test@example.com",
      "password": "DefinitelyWrong@999"
    }' | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Status: {d.get(\"status\")}, Code: {d.get(\"code\")}')"
done
```
**Attempts 1-5**: HTTP `401` "Invalid email or password"  
**Attempt 6**: HTTP `429` "Account temporarily locked due to too many failed attempts. Try again in 15 minutes."

Sample output:
```
--- Attempt 1 ---
Status: 401, Code: UNAUTHORIZED
--- Attempt 2 ---
Status: 401, Code: UNAUTHORIZED
--- Attempt 3 ---
Status: 401, Code: UNAUTHORIZED
--- Attempt 4 ---
Status: 401, Code: UNAUTHORIZED
--- Attempt 5 ---
Status: 401, Code: UNAUTHORIZED
--- Attempt 6 ---
Status: 429, Code: RATE_LIMITED
```

To reset the lockout, you must wait 15 minutes. (In dev, you can restart the app to clear the cache.)

---

### 3.3 Refresh Token — `POST /v1/auth/refresh`

> **Note**: Refresh token is sent in the `Authorization` header, NOT in the request body.

#### ✅ Success case

```bash
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH" \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK` — returns a new access token + a **rotated** refresh token (the old one is invalidated).

#### ❌ Edge Cases

**a) Missing Authorization header**
```bash
curl -s -X POST $BASE/v1/auth/refresh
```
**Expected**: HTTP `401`, code `UNAUTHORIZED`

**b) Invalid token**
```bash
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer invalid-token-12345"
```
**Expected**: HTTP `401`, code `UNAUTHORIZED`, message "Invalid refresh token"

**c) Expired token**
```bash
# The refresh token expires after 7 days. Wait 7 days, then try:
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH"
```
**Expected**: HTTP `401` "Refresh token expired"

**d) Wrong format (no Bearer prefix)**
```bash
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: $REFRESH"
```
**Expected**: HTTP `401`, code `UNAUTHORIZED`

**e) Reusing same token twice (stolen token detection)**
```bash
# First use — succeeds
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH" > /dev/null

# Second use with same token — should fail
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH" | python3 -m json.tool
```
**Expected**: HTTP `401` — token was consumed on first use so it's invalid. All other refresh tokens for this user are also revoked.

---

## 4. Email Verification

### 4.1 Verify Email — `POST /v1/email/verify?token=`

> **Note**: Token is sent as a query parameter, NOT in the request body.

To get a verification token, register a new user and check the app logs:

```bash
# Register a fresh user
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Verify",
    "lastName": "Test",
    "email": "verify.test@example.com",
    "phone": "+260971234100",
    "password": "SecurePass@123"
  }' | python3 -m json.tool > /dev/null

# Extract the verification token from the database
PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
  -t -A \
  -c "SELECT verification_token FROM users WHERE email = 'verify.test@example.com';"
```

If PostgreSQL is not available, check the app logs for the link:
```bash
grep "verify.test@example.com" /tmp/novawallet.log
# Look for: Link: https://novawallet.com/verify-email?token=uuid-here
```

#### ✅ Success case

```bash
TOKEN="the-uuid-from-above"

curl -s -X POST "$BASE/v1/email/verify?token=$TOKEN" \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK`, message "Email verified successfully"

#### ❌ Edge Cases

**a) Invalid token**
```bash
curl -s -X POST "$BASE/v1/email/verify?token=invalid-token-123"
```
**Expected**: HTTP `400`, code `BAD_REQUEST`, message "Invalid or expired verification token"

**b) Already verified email**
```bash
# Try verifying the same email again
curl -s -X POST "$BASE/v1/email/verify?token=$TOKEN"
```
**Expected**: HTTP `400`, code `BAD_REQUEST`, message "already verified"

**c) Missing token param**
```bash
curl -s -X POST "$BASE/v1/email/verify"
```
**Expected**: HTTP `400` (MissingServletRequestParameterException)

**d) Wrong HTTP method**
```bash
curl -s "$BASE/v1/email/verify?token=$TOKEN"
```
**Expected**: HTTP `405` Method Not Allowed (must be POST)

---

## 5. Password Management

### 5.1 Forgot Password — `POST /v1/password/forgot`

#### ✅ Success case (email exists)

```bash
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email": "john.test@example.com"}' \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK`, message "If the email exists, a reset link has been sent"

#### ✅ Success case (email doesn't exist — same response)

```bash
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email": "nonexistent@example.com"}' \
  | python3 -m json.tool
```

**Expected**: Same HTTP `200 OK` with the same message.
> ⚠️ This is intentional — it prevents attackers from discovering which emails are registered.

#### ❌ Edge Cases

**a) Empty email**
```bash
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email": ""}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**b) Invalid email format**
```bash
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email": "not-an-email"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**c) Empty body**
```bash
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

---

### 5.2 Reset Password — `POST /v1/password/reset`

First, get a reset token by calling forgot-password, then extract it:

```bash
# Call forgot-password
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email": "john.test@example.com"}'

# Get the reset token from database
RESET_TOKEN=$(PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
  -t -A \
  -c "SELECT password_reset_token FROM users WHERE email = 'john.test@example.com';")

echo "Reset token: $RESET_TOKEN"
```

If PostgreSQL is not available, check logs:
```bash
grep "Reset password" /tmp/novawallet.log
# Look for: Link: https://novawallet.com/reset-password?token=uuid-here
```

#### ✅ Success case

```bash
curl -s -X POST $BASE/v1/password/reset \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$RESET_TOKEN\",
    \"newPassword\": \"NewSecure@456\"
  }" | python3 -m json.tool
```

**Expected**: HTTP `200 OK`, message "Password reset successful"

After reset, you can login with the new password:
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"NewSecure@456"}'
```
**Expected**: HTTP `200` (login works with new password)

#### ❌ Edge Cases

**a) Invalid token**
```bash
curl -s -X POST $BASE/v1/password/reset \
  -H "Content-Type: application/json" \
  -d '{"token": "invalid-token", "newPassword": "NewSecure@789"}'
```
**Expected**: HTTP `400`, code `BAD_REQUEST`

**b) Expired token** (token expires after 1 hour)
```bash
# Wait 1+ hour, then try with the original token
curl -s -X POST $BASE/v1/password/reset \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$RESET_TOKEN\", \"newPassword\": \"TooLate@999\"}"
```
**Expected**: HTTP `400`, code `BAD_REQUEST`, message "...expired..."

**c) Weak new password**
```bash
curl -s -X POST $BASE/v1/password/reset \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$RESET_TOKEN\", \"newPassword\": \"weak\"}"
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**d) Empty token**
```bash
curl -s -X POST $BASE/v1/password/reset \
  -H "Content-Type: application/json" \
  -d '{"token": "", "newPassword": "NewSecure@789"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

---

## 6. PIN Management

### 6.1 Set PIN — `POST /v1/pin`

> Requires valid JWT (Authorization header).

#### ✅ Success case

```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "2468"}' \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK`, message "PIN set successfully"

#### ❌ Edge Cases

**a) No Authorization header**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -d '{"pin": "2468"}'
```
**Expected**: HTTP `403` Forbidden (no authenticated user)

**b) Invalid JWT**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer invalid.jwt.token" \
  -d '{"pin": "2468"}'
```
**Expected**: HTTP `403` Forbidden

**c) PIN too short (< 4 digits)**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "123"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**d) PIN too long (> 6 digits)**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "1234567"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**e) Non-numeric PIN**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "abcd"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**f) Empty PIN**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": ""}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**g) Empty body**
```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

---

## 7. User Profile

### 7.1 Get Profile — `GET /v1/users/me`

#### ✅ Success case

```bash
curl -s $BASE/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.test@example.com",
    "phone": "+260971234001",
    "role": "USER",
    "emailVerified": true,
    "pinSet": true
  }
}
```

#### ❌ Edge Cases

**a) No auth**
```bash
curl -s $BASE/v1/users/me
```
**Expected**: HTTP `403` Forbidden

**b) Expired JWT** (after 15 min)
```bash
curl -s $BASE/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```
**Expected**: HTTP `403` Forbidden (filter rejects expired token)

---

### 7.2 Update Profile — `PUT /v1/users/me`

#### ✅ Success case

```bash
curl -s -X PUT $BASE/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "UpdatedJohn",
    "phone": "+260971234999"
  }' | python3 -m json.tool
```

**Expected**: HTTP `200 OK` — returns updated profile.

All fields are optional — only send what you want to change.

#### ❌ Edge Cases

**a) Empty name**
```bash
curl -s -X PUT $BASE/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName": ""}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**b) Invalid email format**
```bash
curl -s -X PUT $BASE/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"email": "not-an-email"}'
```
**Expected**: HTTP `400`, `VALIDATION_ERROR`

**c) No changes (empty body)**
```bash
curl -s -X PUT $BASE/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}'
```
**Expected**: HTTP `200 OK` — no changes made, but it's valid.

---

## 8. Admin Endpoints

> All admin endpoints require a user with `ROLE_ADMIN`. Register an admin or use the seeded admin account.

### Seed an admin

```bash
# Register a user, then promote them to admin in the database
PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'john.test@example.com';"
```

Then login as this user:
```bash
ADMIN_TOKEN=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"NewSecure@456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
```

### 8.1 List Users — `GET /v1/admin/users`

```bash
curl -s $BASE/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK` — paginated list of users.

#### ❌ Edge Cases

**a) Non-admin user**
```bash
curl -s $BASE/v1/admin/users \
  -H "Authorization: Bearer $TOKEN"
```
**Expected**: HTTP `403` Forbidden

**b) No auth**
```bash
curl -s $BASE/v1/admin/users
```
**Expected**: HTTP `403` Forbidden

---

### 8.2 Get User — `GET /v1/admin/users/{id}`

```bash
# Get a specific user by UUID
USER_ID="user-uuid-from-list-above"

curl -s $BASE/v1/admin/users/$USER_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK` — user details.

#### ❌ Edge Cases

**a) Non-existent user ID**
```bash
curl -s $BASE/v1/admin/users/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```
**Expected**: HTTP `404` Not Found

**b) Invalid UUID format**
```bash
curl -s $BASE/v1/admin/users/not-a-uuid \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```
**Expected**: HTTP `400`

---

### 8.3 Toggle Delete — `PATCH /v1/admin/users/{id}/toggle-delete`

```bash
curl -s -X PATCH $BASE/v1/admin/users/$USER_ID/toggle-delete \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

**Expected**: HTTP `200 OK`

- First call: soft-deletes the user (sets `deleted_at` timestamp).
- Second call: restores the user (clears `deleted_at`).
- Toggle works both ways.

Test soft-delete effect:
```bash
# After soft-deleting...
# User won't appear in admin list anymore:
curl -s $BASE/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# But GET by ID still works (admin can see deleted users):
curl -s $BASE/v1/admin/users/$USER_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### ❌ Edge Cases

**a) Toggle on already-deleted user**
```bash
# Run twice
curl -s -X PATCH $BASE/v1/admin/users/$USER_ID/toggle-delete \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# User is now deleted

curl -s -X PATCH $BASE/v1/admin/users/$USER_ID/toggle-delete \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# User is now restored
```
**Expected**: Both succeed with `200 OK`.

---

## 9. Rate Limiting Tests

### 9.1 Global Rate Limit (default: 100 req/min)

Send 110 requests rapidly to the health endpoint:

```bash
echo "Testing global rate limit..."
for i in $(seq 1 110); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BASE/actuator/health)
  if [ "$STATUS" = "429" ]; then
    echo "Hit rate limit at request #$i"
    break
  fi
  if [ $((i % 20)) -eq 0 ]; then
    echo "  $i requests sent..."
  fi
done
```

**Expected**: After ~100 requests, starts returning `429 Too Many Requests`.

Check the rate limit headers:
```bash
curl -v $BASE/actuator/health 2>&1 | grep -i "x-ratelimit"
```

**Expected headers**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: <varies>
X-RateLimit-Reset: 60
```

Rate limit response (HTTP 429):
```json
{
  "success": false,
  "message": "Too many requests. Please try again later.",
  "code": "RATE_LIMITED"
}
```

### 9.2 Auth Endpoint Rate Limit (10 req/min)

Auth endpoints (`/v1/auth/*`, `/v1/password/*`, `/v1/pin/*`, `/v1/email/*`) have a stricter limit:

```bash
echo "Testing auth rate limit..."
for i in $(seq 1 15); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST $BASE/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"Test@123"}')
  echo "  Request $i: HTTP $STATUS"
  if [ "$STATUS" = "429" ]; then
    echo "Auth rate limit hit at request #$i!"
    break
  fi
done
```

**Expected**: After ~10 requests, starts returning HTTP `429`.

### 9.3 Login Lockout (5 failed attempts)

Tested in [section 3.2f](#f-login-lockout-5-failed-attempts--15-min-block).

---

## 10. Edge Case Matrix

| Endpoint | Edge Case | Expected Status | Code |
|---|---|---|---|
| **Register** | Duplicate email | 409 | `DUPLICATE_RESOURCE` |
| | Duplicate phone | 409 | `DUPLICATE_RESOURCE` |
| | All fields empty | 400 | `VALIDATION_ERROR` |
| | Invalid email format | 400 | `VALIDATION_ERROR` |
| | Weak password (no special) | 400 | `VALIDATION_ERROR` |
| | Weak password (too short) | 400 | `VALIDATION_ERROR` |
| | Invalid phone | 400 | `VALIDATION_ERROR` |
| | Missing fields | 400 | `VALIDATION_ERROR` |
| | Empty JSON `{}` | 400 | `VALIDATION_ERROR` |
| | Malformed JSON | 400 | (Spring default) |
| **Login** | Wrong password | 401 | `UNAUTHORIZED` |
| | Non-existent email | 401 | `UNAUTHORIZED` |
| | Empty email/password | 400 | `VALIDATION_ERROR` |
| | Empty body | 400 | `VALIDATION_ERROR` |
| | 5 failed attempts | 429 | `RATE_LIMITED` |
| **Refresh** | Missing Authorization header | 401 | `UNAUTHORIZED` |
| | Invalid token | 401 | `UNAUTHORIZED` |
| | No Bearer prefix | 401 | `UNAUTHORIZED` |
| | Reuse consumed token | 401 | `UNAUTHORIZED` |
| **Email Verify** | Invalid token | 400 | `BAD_REQUEST` |
| | Already verified | 400 | `BAD_REQUEST` |
| | Missing token param | 400 | (Spring default) |
| | GET instead of POST | 405 | Method Not Allowed |
| **Forgot Password** | Empty email | 400 | `VALIDATION_ERROR` |
| | Invalid email format | 400 | `VALIDATION_ERROR` |
| **Reset Password** | Invalid token | 400 | `BAD_REQUEST` |
| | Expired token | 400 | `BAD_REQUEST` |
| | Weak new password | 400 | `VALIDATION_ERROR` |
| **Set PIN** | No auth header | 403 | `FORBIDDEN` |
| | Invalid JWT | 403 | `FORBIDDEN` |
| | PIN too short (< 4) | 400 | `VALIDATION_ERROR` |
| | PIN too long (> 6) | 400 | `VALIDATION_ERROR` |
| | Non-numeric PIN | 400 | `VALIDATION_ERROR` |
| | Empty PIN | 400 | `VALIDATION_ERROR` |
| **Get Profile** | No auth | 403 | `FORBIDDEN` |
| | Expired JWT | 403 | `FORBIDDEN` |
| **Update Profile** | Empty name | 400 | `VALIDATION_ERROR` |
| | Invalid email | 400 | `VALIDATION_ERROR` |
| **Admin List** | Non-admin user | 403 | `FORBIDDEN` |
| | No auth | 403 | `FORBIDDEN` |
| **Admin Get User** | Non-existent UUID | 404 | Not Found |
| | Invalid UUID format | 400 | `BAD_REQUEST` |
| **Rate Limit** | Exceed global (100 req/min) | 429 | `RATE_LIMITED` |
| | Exceed auth (10 req/min) | 429 | `RATE_LIMITED` |
| | Exceed login attempts (5) | 429 | `RATE_LIMITED` |

---

## 11. Quick Smoke Test Script

Copy-paste this entire block into your terminal to run a full smoke test:

```bash
#!/bin/bash
# NovaWallet API — Quick Smoke Test
# Sets up test users, runs all endpoints, reports results

BASE=http://localhost:8080/api
PASS=0
FAIL=0

check() {
  local desc="$1"
  local expected="$2"
  local actual="$3"
  if [ "$actual" = "$expected" ]; then
    echo "  ✅ $desc (HTTP $actual)"
    PASS=$((PASS+1))
  else
    echo "  ❌ $desc (expected $expected, got $actual)"
    FAIL=$((FAIL+1))
  fi
}

echo "=== NovaWallet Smoke Test ==="
echo ""

# 1. Health check
echo "--- Health Check ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BASE/actuator/health)
check "Health endpoint" "200" "$STATUS"

# 2. Register
echo "--- Register ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Smoke",
    "lastName": "Test",
    "email": "smoke.test@example.com",
    "phone": "+260971234500",
    "password": "SmokeTest@123"
  }')
check "Register" "201" "$STATUS"

# 3. Register duplicate
echo "--- Duplicate Register ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Smoke",
    "lastName": "Test",
    "email": "smoke.test@example.com",
    "phone": "+260971234501",
    "password": "SmokeTest@123"
  }')
check "Duplicate email" "409" "$STATUS"

# 4. Login
echo "--- Login ---"
RESPONSE=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke.test@example.com","password":"SmokeTest@123"}')
TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)
STATUS=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',200))" 2>/dev/null)
check "Login" "200" "$STATUS"

# 5. Login wrong password
echo "--- Login Wrong Password ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke.test@example.com","password":"WrongPass@999"}')
check "Wrong password → 401" "401" "$STATUS"

# 6. Get profile
echo "--- Profile ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  $BASE/v1/users/me \
  -H "Authorization: Bearer $TOKEN")
check "Get profile" "200" "$STATUS"

# 7. Profile without auth
echo "--- Profile (no auth) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BASE/v1/users/me)
check "Profile without auth → 403" "403" "$STATUS"

# 8. Update profile
echo "--- Update Profile ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X PUT $BASE/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName":"SmokeUpdated"}')
check "Update profile" "200" "$STATUS"

# 9. Set PIN
echo "--- Set PIN ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin":"2468"}')
check "Set PIN" "200" "$STATUS"

# 10. PIN validation — non-numeric
echo "--- PIN Validation ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin":"abcd"}')
check "Non-numeric PIN → 400" "400" "$STATUS"

# 11. Refresh token
echo "--- Token Refresh ---"
REFRESH_TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['refreshToken'])" 2>/dev/null)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN")
check "Refresh token" "200" "$STATUS"

# 12. Forgot password
echo "--- Forgot Password ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke.test@example.com"}')
check "Forgot password" "200" "$STATUS"

# 13. Verify email (mock — will likely return 400 since token changes each time)
echo "--- Email Verify ---"
# Get the verification token from DB
if command -v psql &> /dev/null; then
  VERIFY_TOKEN=$(PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
    -t -A -c "SELECT verification_token FROM users WHERE email='smoke.test@example.com';" 2>/dev/null)
  if [ -n "$VERIFY_TOKEN" ] && [ "$VERIFY_TOKEN" != " " ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "$BASE/v1/email/verify?token=$VERIFY_TOKEN")
    check "Verify email" "200" "$STATUS"
  else
    echo "  ⚠️  Skipping email verify (DB not accessible)"
  fi
else
  echo "  ⚠️  Skipping email verify (psql not installed)"
fi

# 14. Validation error
echo "--- Validation ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{}}')
check "Malformed JSON → 400" "400" "$STATUS"

# Summary
echo ""
echo "=== Results ==="
echo "✅ Passed: $PASS"
echo "❌ Failed: $FAIL"
echo "Total: $((PASS+FAIL))"
```

---

## Tips for Manual Testing

### Working with tokens

```bash
# Save token from login
TOKEN=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Pass@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# Use token
curl -s $BASE/v1/users/me -H "Authorization: Bearer $TOKEN"
```

### Check rate limit headers

```bash
curl -v $BASE/actuator/health 2>&1 | grep -i "^< x-ratelimit"
```

### View email tokens in logs

```bash
# In a separate terminal:
tail -f /tmp/novawallet.log | grep -E "verification|reset|token"

# Or search after a request:
grep "example.com" /tmp/novawallet.log
```

### Reset rate limit counters

Restart the app to clear in-memory Caffeine caches:
```bash
# Kill and restart
lsof -ti:8080 | xargs kill -9
./mvnw spring-boot:run
```

---

## Appendix: Expected Response Snapshots

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed"
}
```

### Validation Error
```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2026-07-10T18:44:19.871257247",
  "path": "/api/v1/auth/register",
  "errors": {
    "password": "Password must contain at least one uppercase letter..."
  }
}
```

### Business Error
```json
{
  "status": 400,
  "code": "BAD_REQUEST",
  "message": "PIN is locked until 2026-07-10T19:00:00",
  "timestamp": "2026-07-10T18:45:00.000000000",
  "path": "/api/v1/pin"
}
```

### Unauthorized
```json
{
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Invalid email or password",
  "timestamp": "2026-07-10T18:44:19.967784941",
  "path": "/api/v1/auth/login"
}
```

### Rate Limited
```json
{
  "success": false,
  "message": "Account temporarily locked due to too many failed attempts. Try again in 15 minutes.",
  "code": "RATE_LIMITED"
}
```

### Duplicate
```json
{
  "status": 409,
  "code": "DUPLICATE_RESOURCE",
  "message": "Email already registered",
  "timestamp": "2026-07-10T18:44:20.000000000",
  "path": "/api/v1/auth/register"
}
```
