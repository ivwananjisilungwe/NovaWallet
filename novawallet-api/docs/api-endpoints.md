# NovaWallet API — Complete Endpoint Reference

> **Version**: Phase 4 (Fee Engine + Audit + AOP)  
> **Base URL**: `http://localhost:8080/api`  
> **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`  
> **OpenAPI JSON**: `http://localhost:8080/api/api-docs`

---

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [Standard Flow](#2-standard-flow)
3. [Authentication](#3-authentication)
   - 3.1 Register — `POST /v1/auth/register`
   - 3.2 Login — `POST /v1/auth/login`
   - 3.3 Refresh Token — `POST /v1/auth/refresh`
4. [Email Verification](#4-email-verification)
   - 4.1 Verify Email — `POST /v1/email/verify`
5. [Password Management](#5-password-management)
   - 5.1 Forgot Password — `POST /v1/password/forgot`
   - 5.2 Reset Password — `POST /v1/password/reset`
6. [PIN Management](#6-pin-management)
   - 6.1 Set PIN — `POST /v1/pin`
7. [User Profile](#7-user-profile)
   - 7.1 Get Profile — `GET /v1/users/me`
   - 7.2 Update Profile — `PUT /v1/users/me`
8. [KYC (Identity Verification)](#8-kyc-identity-verification)
   - 8.1 Upload Document — `POST /v1/kyc/documents/upload`
   - 8.2 Get Status — `GET /v1/kyc/status`
   - 8.3 Submit for Review — `POST /v1/kyc/submit`
9. [Wallet](#9-wallet)
   - 9.1 Get My Wallet — `GET /v1/wallets/me`
10. [Transactions](#10-transactions)
    - 10.1 Deposit — `POST /v1/wallets/{walletId}/deposit`
    - 10.2 Withdraw — `POST /v1/wallets/{walletId}/withdraw`
    - 10.3 Transfer — `POST /v1/transfers`
    - 10.4 History — `GET /v1/wallets/{walletId}/transactions`
    - 10.5 Get by Reference — `GET /v1/transactions/{reference}`
    - 10.6 Balance — `GET /v1/wallets/{walletId}/balance`
11. [Fee Engine](#11-fee-engine)
    - 11.1 Estimate Fee — `GET /v1/fees/estimate`
12. [Admin Endpoints](#12-admin-endpoints)
    - 12.1 List Users — `GET /v1/admin/users`
    - 12.2 Get User — `GET /v1/admin/users/{userId}`
    - 12.3 Toggle Delete — `PATCH /v1/admin/users/{userId}/toggle-delete`
    - 12.4 Pending KYC — `GET /v1/admin/kyc/pending`
    - 12.5 KYC Details — `GET /v1/admin/kyc/{userId}`
    - 12.6 Download Document — `GET /v1/admin/kyc/{userId}/documents/{documentId}`
    - 12.7 Approve KYC — `POST /v1/admin/kyc/{userId}/approve`
    - 12.8 Reject KYC — `POST /v1/admin/kyc/{userId}/reject`
    - 12.9 Freeze Wallet — `POST /v1/admin/wallets/{walletId}/freeze`
    - 12.10 Unfreeze Wallet — `POST /v1/admin/wallets/{walletId}/unfreeze`
    - 12.11 List Fee Configs — `GET /v1/admin/fees`
    - 12.12 Get Fee Config — `GET /v1/admin/fees/{id}`
    - 12.13 Update Fee Config — `PUT /v1/admin/fees/{id}`
13. [Health Check](#13-health-check)
14. [Error Response Format](#14-error-response-format)
15. [Complete Smoke Test Script](#15-complete-smoke-test-script)

---

## 1. Quick Start

### Prerequisites

- Java 21+
- PostgreSQL running on `localhost:5432`
- Database named `novawallet` (Flyway auto-creates tables)

PostgreSQL connection defaults:
```
Host:     localhost:5432
Database: novawallet
Username: postgres
Password: 8407
```

### Start the API

```bash
cd /media/ivwananji-silungwe/DEVELOPMENT/PROGRAMMING/PROJECTS/NovaWallet/novawallet-api

# Build (skip tests for faster startup)
./mvnw clean package -DskipTests

# Start
./mvnw spring-boot:run
```

### Verify it's running

```bash
curl -s http://localhost:8080/api/actuator/health
# → {"status":"UP"}
```

### Save base URL

```bash
BASE=http://localhost:8080/api
```

### Helper for pretty JSON

```bash
alias pp='python3 -m json.tool'

extract() {
  python3 -c "import sys,json; print(json.load(sys.stdin)['data']['$1'])"
}
```

### Admin Credentials

| Field | Value |
|---|---|
| Email | `admin@novawallet.com` |
| Password | `Admin@123` |
| First Name | `Admin` |
| Last Name | `User` |
| Phone | `+260000000000` |
| Role | `ADMIN` |

The admin user is bootstrapped automatically on first startup by `AdminDataInitializer`.

### Test User Credentials (you create)

Throughout this guide we use:

| Variable | Value |
|---|---|
| `EMAIL` | `john.test@example.com` |
| `PASSWORD` | `SecurePass@123` |
| `PHONE` | `+260971234001` |
| `PIN` | `2468` |

---

## 2. Standard Flow

The typical user journey follows this order:

```
Register → Verify Email → Set PIN → Upload KYC Docs → Submit KYC → Admin Approves → Wallet Created → Deposit → Withdraw/Transfer
```

**Important**: A wallet is **not** created on registration. It is created automatically only after an admin approves the user's KYC submission. This is a fintech compliance requirement.

---

## 3. Authentication

### 3.1 Register — `POST /v1/auth/register`

Creates a new user account. Returns JWT tokens for immediate use.

**Request body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.test@example.com",
  "phone": "+260971234001",
  "password": "SecurePass@123"
}
```

| Field | Rules |
|---|---|
| `firstName` | 1-100 chars, required |
| `lastName` | 1-100 chars, required |
| `email` | Valid email, required, must be unique |
| `phone` | `+` followed by 10-15 digits, required, must be unique |
| `password` | 8-128 chars, must have uppercase + lowercase + digit + special char |

#### ✅ Success

```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.test@example.com",
    "phone": "+260971234001",
    "password": "SecurePass@123"
  }' | pp
```

**Response** `201 Created`:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "expiresIn": 900000,
    "user": {
      "id": "uuid...",
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

#### ❌ Duplicate email — `409 Conflict`

```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "john.test@example.com",
    "phone": "+260971234002",
    "password": "SecurePass@123"
  }' | pp
```
→ `409`, `DUPLICATE_RESOURCE`, "Email already registered"

#### ❌ Duplicate phone — `409 Conflict`

```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.test@example.com",
    "phone": "+260971234001",
    "password": "SecurePass@123"
  }' | pp
```
→ `409`, `DUPLICATE_RESOURCE`, "Phone number already registered"

#### ❌ Weak password — `400 Bad Request`

```bash
curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.weak@example.com",
    "phone": "+260971234010",
    "password": "securepass123"
  }' | pp
```
→ `400`, `VALIDATION_ERROR` with per-field messages

---

### 3.2 Login — `POST /v1/auth/login`

Authenticates with email and password. Returns JWT tokens.

**Request body:**
```json
{
  "email": "john.test@example.com",
  "password": "SecurePass@123"
}
```

#### ✅ Success

```bash
# Save tokens for later use
RESPONSE=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"SecurePass@123"}')

TOKEN=$(echo $RESPONSE | extract accessToken)
REFRESH=$(echo $RESPONSE | extract refreshToken)

echo "Token:   ${TOKEN:0:50}..."
echo "Refresh: ${REFRESH:0:50}..."
```

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "uuid...",
    "expiresIn": 900000,
    "user": { ... }
  },
  "message": "Login successful"
}
```

#### ❌ Invalid credentials — `401 Unauthorized`

```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"WrongPassword@456"}' | pp
```
→ `401`, `UNAUTHORIZED`, "Invalid email or password"

> Same message for wrong password AND non-existent email — prevents user enumeration.

#### ❌ Login lockout — `429 Rate Limited`

After 5 failed attempts, the account is locked for 15 minutes:
```bash
for i in $(seq 1 6); do
  echo "--- Attempt $i ---"
  curl -s -X POST $BASE/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"john.test@example.com","password":"WrongPassword@999"}' | \
    python3 -c "import sys,json; d=json.load(sys.stdin); print(f'HTTP {d.get(\"status\")}: {d.get(\"code\")}')"
done
```
```
Attempt 1-5: 401, UNAUTHORIZED
Attempt 6:   429, RATE_LIMITED — "Account temporarily locked..."
```

---

### 3.3 Refresh Token — `POST /v1/auth/refresh`

Exchanges a valid refresh token for a new access token + rotated refresh token. The old refresh token is invalidated.

> **Note**: Token goes in the `Authorization` header, NOT the body.

#### ✅ Success

```bash
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH" | pp
```

#### ❌ Missing header — `401`

```bash
curl -s -X POST $BASE/v1/auth/refresh | pp
```

#### ❌ Reuse (stolen token detection) — `401`

The second call with the same refresh token always fails, and all other refresh tokens for that user are revoked:
```bash
# First use — succeeds
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH" > /dev/null

# Second use with same token — fails
curl -s -X POST $BASE/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH" | pp
```
→ `401`, `UNAUTHORIZED`

---

## 4. Email Verification

### 4.1 Verify Email — `POST /v1/email/verify?token=`

In dev mode, emails are logged to the console. Get the verification token from the database or logs:

```bash
# From database
PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
  -t -A -c "SELECT verification_token FROM users WHERE email = 'john.test@example.com';"

# Or from app logs
grep "verify" /tmp/novawallet.log | tail -5
```

#### ✅ Success

```bash
TOKEN="uuid-from-above"
curl -s -X POST "$BASE/v1/email/verify?token=$TOKEN" | pp
```
→ `200 OK`, "Email verified successfully"

#### ❌ Invalid token — `400`

```bash
curl -s -X POST "$BASE/v1/email/verify?token=invalid-token" | pp
```

---

## 5. Password Management

### 5.1 Forgot Password — `POST /v1/password/forgot`

Always returns success (even if email doesn't exist) to prevent email enumeration.

```bash
curl -s -X POST $BASE/v1/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com"}' | pp
```
→ `200 OK`, "If the email exists, a reset link has been sent"

Get the reset token:
```bash
PGPASSWORD=8407 psql -h localhost -U postgres -d novawallet \
  -t -A -c "SELECT password_reset_token FROM users WHERE email = 'john.test@example.com';"
```

### 5.2 Reset Password — `POST /v1/password/reset`

```bash
RESET_TOKEN="uuid-from-above"
curl -s -X POST $BASE/v1/password/reset \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$RESET_TOKEN\",
    \"newPassword\": \"NewSecure@456\"
  }" | pp
```
→ `200 OK`, "Password reset successful"

Login with new password:
```bash
curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.test@example.com","password":"NewSecure@456"}' | pp
```

---

## 6. PIN Management

### 6.1 Set PIN — `POST /v1/pin`

Sets a 4-6 digit transaction PIN. **Required before withdrawing or transferring funds.**

> **Auth**: `JWT` (Bearer token)

```bash
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "2468"}' | pp
```
→ `200 OK`, "PIN set successfully"

#### Validation

| Input | Result |
|---|---|
| `"123"` (3 digits) | `400` — too short |
| `"1234567"` (7 digits) | `400` — too long |
| `"abcd"` (letters) | `400` — must be numeric |
| No `Authorization` header | `403` Forbidden |

---

## 7. User Profile

### 7.1 Get Profile — `GET /v1/users/me`

> **Auth**: `JWT`

```bash
curl -s $BASE/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | pp
```

**Response**:
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
  },
  "message": "Profile retrieved"
}
```

### 7.2 Update Profile — `PUT /v1/users/me`

> **Auth**: `JWT`

All fields are optional (send only what you want to change).

```bash
curl -s -X PUT $BASE/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName": "Jonathan", "phone": "+260971234099"}' | pp
```

---

## 8. KYC (Identity Verification)

KYC must be completed before a wallet is created. Steps:
1. Upload required documents (JPEG, PNG, WebP, PDF; max 10MB each)
2. Submit for review
3. Admin approves → wallet is auto-created

### 8.1 Upload Document — `POST /v1/kyc/documents/upload`

> **Auth**: `JWT`  
> **Content-Type**: `multipart/form-data`

**Document types**: `NATIONAL_ID`, `SELFIE`, `PROOF_OF_ADDRESS`, `PASSPORT`

```bash
# Create a sample file
echo "fake id content" > /tmp/national_id.jpg

curl -s -X POST $BASE/v1/kyc/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=NATIONAL_ID" \
  -F "file=@/tmp/national_id.jpg" | pp
```

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "documentType": "NATIONAL_ID",
    "fileName": "national_id.jpg",
    "uploadedAt": "2026-07-10T12:00:00"
  },
  "message": "Document uploaded successfully"
}
```

Upload more documents for higher tiers:
```bash
echo "fake selfie" > /tmp/selfie.png
curl -s -X POST $BASE/v1/kyc/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=SELFIE" \
  -F "file=@/tmp/selfie.png" > /dev/null
```

### 8.2 Get Status — `GET /v1/kyc/status`

> **Auth**: `JWT`

```bash
curl -s $BASE/v1/kyc/status \
  -H "Authorization: Bearer $TOKEN" | pp
```

### 8.3 Submit for Review — `POST /v1/kyc/submit`

> **Auth**: `JWT`

Submits all uploaded documents for admin review. Cannot upload more documents until review is complete.

```bash
curl -s -X POST $BASE/v1/kyc/submit \
  -H "Authorization: Bearer $TOKEN" | pp
```
→ `200 OK`, "KYC documents submitted for review"

---

## 9. Wallet

### 9.1 Get My Wallet — `GET /v1/wallets/me`

> **Auth**: `JWT`

Returns the authenticated user's wallet. If KYC has not been approved, the wallet won't exist yet.

```bash
curl -s $BASE/v1/wallets/me \
  -H "Authorization: Bearer $TOKEN" | pp
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "balance": 0.00,
    "currency": "ZMW",
    "status": "ACTIVE",
    "kycTier": 2,
    "dailySendLimit": 20000.00,
    "walletLimit": 50000.00,
    "createdAt": "2026-07-10T12:00:00"
  },
  "message": "Wallet retrieved"
}
```

Status values: `ACTIVE`, `FROZEN`

---

## 10. Transactions

### 10.1 Deposit — `POST /v1/wallets/{walletId}/deposit`

> **Auth**: `JWT`

Adds funds to a wallet. Does **not** require a PIN.

```bash
curl -s -X POST "$BASE/v1/wallets/$WALLET_ID/deposit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "amount": 100.00,
    "description": "Cash deposit at branch"
  }' | pp
```

**Response** `201 Created`:
```json
{
  "success": true,
  "data": {
    "reference": "NWTX2026071000000001",
    "type": "DEPOSIT",
    "status": "COMPLETED",
    "amount": 100.00,
    "fee": 1.00,
    "description": "Cash deposit at branch",
    "senderWalletId": null,
    "receiverWalletId": "uuid",
    "createdAt": "2026-07-10T12:00:00"
  },
  "message": "Deposit successful"
}
```

### 10.2 Withdraw — `POST /v1/wallets/{walletId}/withdraw`

> **Auth**: `JWT`  
> **Requires**: PIN (set via `POST /v1/pin`)

```bash
curl -s -X POST "$BASE/v1/wallets/$WALLET_ID/withdraw" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "amount": 50.00,
    "pin": "2468",
    "description": "ATM withdrawal"
  }' | pp
```

**Response** `201 Created`:
```json
{
  "success": true,
  "data": {
    "reference": "NWTX2026071000000002",
    "type": "WITHDRAWAL",
    "status": "COMPLETED",
    "amount": 50.00,
    "fee": 0.50,
    "description": "ATM withdrawal",
    "createdAt": "2026-07-10T12:00:00"
  }
}
```

### 10.3 Transfer — `POST /v1/transfers`

> **Auth**: `JWT`  
> **Requires**: PIN

Transfers funds from your wallet to another user's wallet. A fee is deducted from the sender.

```bash
# Get receiver's wallet ID first
RECEIVER_WALLET_ID="uuid-of-receiver-wallet"

curl -s -X POST "$BASE/v1/transfers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"receiverWalletId\": \"$RECEIVER_WALLET_ID\",
    \"amount\": 25.00,
    \"pin\": \"2468\",
    \"description\": \"Payment for services\"
  }" | pp
```

**Response** `201 Created`:
```json
{
  "success": true,
  "data": {
    "reference": "NWTX2026071000000003",
    "type": "TRANSFER",
    "status": "COMPLETED",
    "amount": 25.00,
    "fee": 0.50,
    "description": "Payment for services",
    "senderWalletId": "uuid",
    "receiverWalletId": "uuid",
    "createdAt": "2026-07-10T12:00:00"
  }
}
```

#### ❌ Edge Cases

| Scenario | Input | Expected |
|---|---|---|
| Self-transfer | Your own wallet ID | `400` "Cannot transfer to own wallet" |
| Non-existent receiver | Random UUID | `404` "Receiver wallet not found" |
| Wrong PIN | `"0000"` | `400` "Invalid PIN" |
| Insufficient balance | More than balance+fees | `400` "Insufficient balance" |
| Frozen wallet | Admin-frozen wallet | `400` "Wallet is not active" |

### 10.4 Transaction History — `GET /v1/wallets/{walletId}/transactions`

> **Auth**: `JWT`

Supports optional filtering by type, status, and date range.

```bash
curl -s "$BASE/v1/wallets/$WALLET_ID/transactions?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | pp
```

With filters:
```bash
curl -s "$BASE/v1/wallets/$WALLET_ID/transactions?type=DEPOSIT&status=COMPLETED&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | pp
```

| Parameter | Type | Default |
|---|---|---|
| `type` | `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`, `FEE` | all |
| `status` | `PENDING`, `COMPLETED`, `FAILED` | all |
| `from` | ISO datetime (`2026-07-01T00:00:00`) | none |
| `to` | ISO datetime | none |
| `page` | int | 0 |
| `size` | int | 20 |

### 10.5 Get Transaction by Reference — `GET /v1/transactions/{reference}`

> **Auth**: `JWT`

```bash
curl -s "$BASE/v1/transactions/NWTX2026071000000001" \
  -H "Authorization: Bearer $TOKEN" | pp
```

### 10.6 Get Wallet Balance — `GET /v1/wallets/{walletId}/balance`

> **Auth**: `JWT`

```bash
curl -s "$BASE/v1/wallets/$WALLET_ID/balance" \
  -H "Authorization: Bearer $TOKEN" | pp
```

---

## 11. Fee Engine

### 11.1 Estimate Fee — `GET /v1/fees/estimate`

Calculate the estimated fee for a transaction without executing it.

```bash
curl -s "$BASE/v1/fees/estimate?type=TRANSFER&amount=100.00" \
  -H "Authorization: Bearer $TOKEN" | pp
```

**Response**:
```json
{
  "success": true,
  "data": {
    "transactionType": "TRANSFER",
    "amount": 100.00,
    "fee": 2.00,
    "totalDeduction": 102.00,
    "feeDescription": "1.0% + 1.00 flat"
  }
}
```

| Transaction Type | Fee (default config) |
|---|---|
| `DEPOSIT` | 1.0% + 0.00 flat |
| `WITHDRAWAL` | 0.5% + 1.00 flat |
| `TRANSFER` | 1.0% + 1.00 flat |

---

## 12. Admin Endpoints

All admin endpoints require ADMIN role. Use the admin credentials to authenticate.

```bash
# Login as admin
ADMIN_RESP=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@novawallet.com","password":"Admin@123"}')

ADMIN_TOKEN=$(echo $ADMIN_RESP | extract accessToken)
```

### 12.1 List Users — `GET /v1/admin/users`

```bash
curl -s $BASE/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.2 Get User — `GET /v1/admin/users/{userId}`

```bash
curl -s $BASE/v1/admin/users/$USER_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.3 Toggle Delete — `PATCH /v1/admin/users/{userId}/toggle-delete`

Soft-deletes or restores a user account.

```bash
curl -s -X PATCH "$BASE/v1/admin/users/$USER_ID/toggle-delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.4 Pending KYC — `GET /v1/admin/kyc/pending`

Lists all users whose KYC is pending review.

```bash
curl -s $BASE/v1/admin/kyc/pending \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.5 KYC Details — `GET /v1/admin/kyc/{userId}`

```bash
curl -s "$BASE/v1/admin/kyc/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.6 Download Document — `GET /v1/admin/kyc/{userId}/documents/{documentId}`

```bash
curl -s -o /tmp/kyc_doc.pdf \
  "$BASE/v1/admin/kyc/$USER_ID/documents/$DOC_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 12.7 Approve KYC — `POST /v1/admin/kyc/{userId}/approve`

**This creates the user's wallet automatically.**

```bash
curl -s -X POST "$BASE/v1/admin/kyc/$USER_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"tier": 2}' | pp
```
→ `200 OK`, "KYC approved and wallet created"

| Tier | Name | Wallet Limit | Daily Send Limit |
|---|---|---|---|
| 1 | Basic | 5,000 ZMW | 2,000 ZMW |
| 2 | Standard | 50,000 ZMW | 20,000 ZMW |
| 3 | Advanced | 200,000 ZMW | 100,000 ZMW |

### 12.8 Reject KYC — `POST /v1/admin/kyc/{userId}/reject`

User can re-upload documents and resubmit.

```bash
curl -s -X POST "$BASE/v1/admin/kyc/$USER_ID/reject" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"reason": "ID document is blurry. Please upload a clearer photo."}' | pp
```

### 12.9 Freeze Wallet — `POST /v1/admin/wallets/{walletId}/freeze`

Prevents all transactions on the wallet.

| Freeze Reason | Description |
|---|---|
| `SUSPICIOUS_ACTIVITY` | Fraud or unusual patterns |
| `ADMIN_ACTION` | Administrative hold |
| `USER_REQUEST` | Requested by the user |

```bash
curl -s -X POST "$BASE/v1/admin/wallets/$WALLET_ID/freeze" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"reason": "SUSPICIOUS_ACTIVITY"}' | pp
```

After freeze, deposits fail:
```bash
curl -s -X POST "$BASE/v1/wallets/$WALLET_ID/deposit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 10.00, "description": "Test"}' | pp
```
→ `400`, "Wallet is not active"

### 12.10 Unfreeze Wallet — `POST /v1/admin/wallets/{walletId}/unfreeze`

```bash
curl -s -X POST "$BASE/v1/admin/wallets/$WALLET_ID/unfreeze" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.11 List Fee Configurations — `GET /v1/admin/fees`

```bash
curl -s $BASE/v1/admin/fees \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.12 Get Fee Configuration — `GET /v1/admin/fees/{id}`

```bash
curl -s "$BASE/v1/admin/fees/$FEE_CONFIG_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | pp
```

### 12.13 Update Fee Configuration — `PUT /v1/admin/fees/{id}`

```bash
curl -s -X PUT "$BASE/v1/admin/fees/$FEE_CONFIG_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "percentageFee": 1.5,
    "flatFee": 0.00,
    "minFee": 0.50,
    "maxFee": 50.00,
    "active": true
  }' | pp
```

---

## 13. Health Check

```bash
curl -s $BASE/actuator/health | pp
# → {"status":"UP"}
```

---

## 14. Error Response Format

All errors follow a consistent JSON format:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2026-07-10T18:44:19.871",
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
| **400** | `BAD_REQUEST` | Business logic error (invalid PIN, already verified, expired token, insufficient balance). |
| **401** | `UNAUTHORIZED` | Bad credentials, invalid/expired JWT, missing Authorization header. |
| **403** | `FORBIDDEN` | Authenticated but not authorized (non-admin on admin endpoint). |
| **404** | `NOT_FOUND` | Resource not found (user, wallet, transaction). |
| **409** | `DUPLICATE_RESOURCE` | Email or phone already registered. |
| **429** | `RATE_LIMITED` | Too many requests. Login lockout: 5 failed attempts, 15 min wait. |
| **500** | `INTERNAL_SERVER_ERROR` | Unexpected server error. |

---

## 15. Complete Smoke Test Script

Save this as `smoke.sh` and run it to verify everything end-to-end:

```bash
#!/bin/bash
set -e

BASE=http://localhost:8080/api
PP="python3 -m json.tool"
EXTRACT='python3 -c "import sys,json; print(json.load(sys.stdin)[\"data\"][\"$1\"])"'

echo "=== 1. Health Check ==="
curl -s $BASE/actuator/health | $PP

echo -e "\n=== 2. Register User ==="
EMAIL="smoke.$(date +%s)@example.com"
RESP=$(curl -s -X POST $BASE/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"firstName\": \"Smoke\",
    \"lastName\": \"Test\",
    \"email\": \"$EMAIL\",
    \"phone\": \"+26097$(date +%s)\",
    \"password\": \"SmokeTest@123\"
  }")
echo "$RESP" | $PP
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
USER_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['user']['id'])")

echo -e "\n=== 3. Set PIN ==="
curl -s -X POST $BASE/v1/pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pin": "2468"}' | $PP

echo -e "\n=== 4. Upload KYC Documents ==="
echo "test" > /tmp/smoke_id.jpg
curl -s -X POST $BASE/v1/kyc/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=NATIONAL_ID" \
  -F "file=@/tmp/smoke_id.jpg" | $PP
echo "test" > /tmp/smoke_selfie.png
curl -s -X POST $BASE/v1/kyc/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=SELFIE" \
  -F "file=@/tmp/smoke_selfie.png" | $PP

echo -e "\n=== 5. Submit KYC ==="
curl -s -X POST $BASE/v1/kyc/submit \
  -H "Authorization: Bearer $TOKEN" | $PP

echo -e "\n=== 6. Admin Login ==="
ADMIN_RESP=$(curl -s -X POST $BASE/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@novawallet.com","password":"Admin@123"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
echo "Admin token obtained"

echo -e "\n=== 7. Approve KYC (creates wallet) ==="
curl -s -X POST "$BASE/v1/admin/kyc/$USER_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"tier": 2}' | $PP

echo -e "\n=== 8. Get Wallet ==="
WALLET_RESP=$(curl -s $BASE/v1/wallets/me \
  -H "Authorization: Bearer $TOKEN")
echo "$WALLET_RESP" | $PP
WALLET_ID=$(echo "$WALLET_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo -e "\n=== 9. Deposit ==="
curl -s -X POST "$BASE/v1/wallets/$WALLET_ID/deposit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 200.00, "description": "Smoke test deposit"}' | $PP

echo -e "\n=== 10. Check Balance ==="
curl -s "$BASE/v1/wallets/$WALLET_ID/balance" \
  -H "Authorization: Bearer $TOKEN" | $PP

echo -e "\n=== 11. Withdraw ==="
curl -s -X POST "$BASE/v1/wallets/$WALLET_ID/withdraw" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 50.00, "pin": "2468", "description": "Smoke test withdrawal"}' | $PP

echo -e "\n=== 12. Check Balance (after withdrawal) ==="
curl -s "$BASE/v1/wallets/$WALLET_ID/balance" \
  -H "Authorization: Bearer $TOKEN" | $PP

echo -e "\n=== 13. Transaction History ==="
curl -s "$BASE/v1/wallets/$WALLET_ID/transactions?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | $PP

echo -e "\n=== 14. Fee Estimate ==="
curl -s "$BASE/v1/fees/estimate?type=TRANSFER&amount=100.00" \
  -H "Authorization: Bearer $TOKEN" | $PP

echo -e "\n=== ALL DONE ==="
```

Run it:
```bash
chmod +x smoke.sh && ./smoke.sh
```
