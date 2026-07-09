# NovaWallet API — Testing Guide

**Base URL**: http://localhost:8080/api  
**Swagger UI**: http://localhost:8080/api/swagger-ui.html

---

## Test Credentials

### Regular User
| Field | Value |
|-------|-------|
| Email | `testuser@example.com` |
| Password | `TestPass123` |
| Phone | `+260971234999` |
| PIN | `1234` |
| Role | USER |

### Admin User
| Field | Value |
|-------|-------|
| Email | `admin@example.com` |
| Password | `AdminPass123` |
| Phone | `+260971234998` |
| Role | ADMIN |

---

## Flow 1: Registration & Authentication

### 1.1 Register a new user

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "phone": "+260971234001",
    "password": "MySecurePass1"
  }'
```

**Expected**: `201 Created` — returns access token, refresh token, and user info.

### 1.2 Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPass123"
  }'
```

**Expected**: `200 OK` — returns tokens and user info. Save the `accessToken` for subsequent requests.

### 1.3 Refresh token

When the access token expires (after 15 min), use the refresh token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Authorization: Bearer <refreshToken>"
```

**Expected**: `200 OK` — returns new access token and a rotated refresh token.

### 1.4 Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <accessToken>"
```

**Expected**: `200 OK` — revokes all refresh tokens for the user.

---

## Flow 2: PIN Management

### 2.1 Set a PIN (4-6 digits)

```bash
curl -X POST http://localhost:8080/api/v1/auth/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"pin": "1234"}'
```

**Expected**: `200 OK`

**Validation**: PIN must be exactly 4-6 numeric digits. Non-numeric, shorter, or longer values return `400 Bad Request`.

### 2.2 Verify PIN

```bash
curl -X POST http://localhost:8080/api/v1/auth/pin/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"pin": "1234"}'
```

**Expected**: `200 OK` on correct PIN.

### 2.3 Trigger PIN lockout

Send wrong PIN 3 times:

```bash
for i in 1 2 3; do
  curl -s -X POST http://localhost:8080/api/v1/auth/pin/verify \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <accessToken>" \
    -d '{"pin": "9999"}' | python3 -m json.tool
done
```

**3rd attempt expected**: `400 Bad Request` with message "PIN is locked until ..." — account locked for 15 minutes.

---

## Flow 3: User Profile

### 3.1 Get profile

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"
```

**Expected**: `200 OK` — returns user profile info.

### 3.2 Update profile

```bash
curl -X PUT http://localhost:8080/api/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "firstName": "Updated",
    "phone": "+260971234002"
  }'
```

**Expected**: `200 OK` — returns updated profile. Fields are optional — only send what you want to change.

---

## Flow 4: Wallet

### 4.1 Get wallet

```bash
curl http://localhost:8080/api/v1/wallets/me \
  -H "Authorization: Bearer <accessToken>"
```

**Expected**: `200 OK` — returns wallet with account number (starts with `NW`), balance, currency (ZMW), and status.

Example response:
```json
{
  "success": true,
  "data": {
    "id": "bb617359-...",
    "accountNumber": "NW5656485931",
    "balance": 0.00,
    "currency": "ZMW",
    "status": "ACTIVE"
  }
}
```

---

## Flow 5: Email Verification

```bash
curl "http://localhost:8080/api/v1/auth/verify?token=test-verify-token-123"
```

**Expected**: `200 OK`

Test with invalid/expired token:
```bash
curl "http://localhost:8080/api/v1/auth/verify?token=invalid-token"
```

**Expected**: `400 Bad Request`

---

## Flow 6: Password Reset

### 6.1 Request password reset

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "testuser@example.com"}'
```

**Expected**: `200 OK` (always returns success even if email doesn't exist — prevents email enumeration)

### 6.2 Reset password

After requesting a reset, look up the token from the database:
```bash
PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
  -c "SELECT password_reset_token FROM users WHERE email = 'testuser@example.com';"
```

Then use that token:
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<password_reset_token_from_db>",
    "newPassword": "MyNewPass456"
  }'
```

**Expected**: `200 OK`

---

## Flow 7: Admin Endpoints

All admin endpoints require `ROLE_ADMIN`. Use the admin credentials.

### 7.1 Login as admin

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "AdminPass123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
```

### 7.2 List all users

```bash
curl http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected**: `200 OK` — returns all active (non-deleted) users.

### 7.3 Get user by ID

```bash
curl http://localhost:8080/api/v1/admin/users/<userId> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 7.4 Soft-delete / restore a user

```bash
# Soft-delete: user becomes invisible to regular queries
curl -X PATCH http://localhost:8080/api/v1/admin/users/<userId>/toggle-delete \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Run again to restore (toggle)
curl -X PATCH http://localhost:8080/api/v1/admin/users/<userId>/toggle-delete \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected**: `200 OK` — toggles the user's deleted status.

---

## Error Responses Cheat Sheet

| Status | Code | When |
|--------|------|------|
| 201 | — | Registration successful |
| 200 | — | Successful operation |
| 400 | `VALIDATION_ERROR` | Invalid input (missing fields, bad format, short password, non-numeric PIN) |
| 400 | `BAD_REQUEST` | PIN locked, invalid PIN, email already verified, reset token expired |
| 401 | `UNAUTHORIZED` | Wrong email/password, invalid/expired JWT, malformed Authorization header |
| 403 | `FORBIDDEN` | Non-admin accessing admin endpoints |
| 409 | `DUPLICATE_RESOURCE` | Email or phone already registered |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected server error (check logs) |

All error responses include a `path` field and `timestamp`.

---

## Validation Rules

| Field | Rule |
|-------|------|
| First/Last name | 1-100 characters |
| Email | Valid email format |
| Phone | `+` followed by 10-15 digits (e.g., `+260971234567`) |
| Password | 8-128 characters |
| PIN | Exactly 4-6 numeric digits |
| Profile updates | All fields optional, same validation per field |

---

## Quick Smoke Test (copy-paste this entire block)

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "testuser@example.com", "password": "TestPass123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo "Token: ${TOKEN:0:30}..."

# 2. Get profile
echo "=== PROFILE ==="
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 3. Get wallet
echo "=== WALLET ==="
curl -s http://localhost:8080/api/v1/wallets/me \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 4. Set PIN
echo "=== SET PIN ==="
curl -s -X POST http://localhost:8080/api/v1/auth/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "2468"}' | python3 -m json.tool

# 5. Verify PIN
echo "=== VERIFY PIN ==="
curl -s -X POST http://localhost:8080/api/v1/auth/pin/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "2468"}' | python3 -m json.tool

# 6. Update profile
echo "=== UPDATE PROFILE ==="
curl -s -X PUT http://localhost:8080/api/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName": "UpdatedName"}' | python3 -m json.tool

# 7. Admin login
echo "=== ADMIN LOGIN ==="
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "AdminPass123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 8. Admin list users
echo "=== ADMIN LIST USERS ==="
curl -s http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool
```
