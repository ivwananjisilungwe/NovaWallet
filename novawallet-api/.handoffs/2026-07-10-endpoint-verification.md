# Session Handoff — Endpoint Verification & Bug Fixes

**Date:** 2026-07-10 (continued)
**Branch:** `master` (head: `509794b`)

## What Changed This Session

### 1. Fixed Pre-Existing Test Failures
- `NovawalletApiApplicationTests` — added `@ActiveProfiles("test")` to use H2 in-memory DB
- `TransactionFlowIntegrationTest` — updated expected balances to account for fees (48.50), updated transaction count (6)
- **Result: 85/85 tests passing**

### 2. Fixed TransactionController URL Path Bug
- `TransactionController` had `@RequestMapping("/api/v1")` while context-path is already `/api`
- **Result:** Transaction endpoints were producing `/api/api/v1/...` instead of `/api/v1/...`
- **Fix:** Changed to `@RequestMapping("/v1")` to match all other controllers

### 3. Verified All 24 Endpoints via HTTP
Complete fintech flow tested with real HTTP requests:

| # | Endpoint | Result |
|---|----------|--------|
| 1 | GET /api/actuator/health | ✅ 200 |
| 2 | POST /api/v1/auth/register | ✅ 201 |
| 3 | POST /api/v1/pin | ✅ 200 |
| 4 | POST /api/v1/kyc/documents/upload | ✅ 200 |
| 5 | POST /api/v1/kyc/submit | ✅ 200 |
| 6 | GET /api/v1/wallets/me (pre-KYC) | ✅ 404 (correct — no wallet yet) |
| 7 | POST /api/v1/admin/kyc/{id}/approve | ✅ 200 (wallet auto-created) |
| 8 | GET /api/v1/wallets/me (post-KYC) | ✅ 200 |
| 9 | POST /api/v1/wallets/{id}/deposit | ✅ 201 |
| 10 | GET /api/v1/wallets/{id}/balance | ✅ 200 |
| 11 | POST /api/v1/wallets/{id}/withdraw | ✅ 201 |
| 12-15 | User2 full KYC flow | ✅ 201/200 |
| 16 | POST /api/v1/transfers | ✅ 201 |
| 17 | GET /api/v1/wallets/{id}/transactions | ✅ 200 |
| 18 | POST /api/v1/auth/refresh | ✅ 200 |
| 19 | Wrong PIN → 400 | ✅ |
| 20 | Cross-wallet access → 403 | ✅ |
| 21 | No token → 403 | ✅ |
| 22 | Self-transfer → 400 | ✅ |

### 4. Git State
```
509794b fix: TransactionController URL path inconsistency + endpoint verification
4f32431 fix: resolve 2 pre-existing test failures — 85/85 tests passing
aa1a0ae Phase 4: Complete NovaWallet implementation
```

### 5. Known Design Decision (CONFIRMED CORRECT)
**Wallets are only created after KYC approval** — not during registration. This is the correct fintech pattern. The flow is: Register → Upload KYC docs → Submit for review → Admin approves → Wallet auto-created in `AdminKycService.approveKyc()`.
