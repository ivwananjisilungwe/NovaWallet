# NovaWallet API Endpoint Inventory & Phase 1D Coverage

**Repository**: `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet/novawallet-api`
**Stack**: Spring Boot 3.5.3, Java 17
**Total @RestController classes**: 15 + 1 `@RestControllerAdvice`
**Base path prefix**: `/api/v1`

---
## Endpoint Inventory Table (Grouped by Domain)

### 1. Auth & Identity
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `POST /api/v1/auth/register` | register | None (permit-all) | Creates new user account with wallet; returns JWT tokens |
| `POST /api/v1/auth/login` | login | None (permit-all) | Authenticates with email/password; returns JWT access+refresh tokens |
| `POST /api/v1/auth/refresh` | refresh | None (permit-all) | Exchanges refresh token for new access+rotated refresh token |
| `POST /api/v1/email/verify` | verifyEmail | None | Verifies email using token sent during registration |
| `POST /api/v1/pin` | setPin | JWT | Sets 4-6 digit transaction PIN for authenticated user |
| `POST /api/v1/pin/change` | changePin | JWT | Changes PIN; requires current PIN + new 4-6 digit PIN |
| `GET /api/v1/users/me` | getProfile | JWT | Returns authenticated user's profile information |
| `PUT /api/v1/users/me` | updateProfile | JWT | Updates profile (first name, last name, phone) |
| `POST /api/v1/users/me/change-password` | changePassword | JWT | Changes password (requires current password, new min 8 chars) |

### 2. KYC
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `POST /api/v1/kyc/documents/upload` | uploadDocument | JWT | Upload KYC document (National ID, Passport, Selfie, Proof of Address). Max 10MB, jpeg/png/webp/pdf |
| `GET /api/v1/kyc/status` | getKycStatus | JWT | Returns KYC status, tier, limits, and uploaded documents for authenticated user |
| `POST /api/v1/kyc/submit` | submitForReview | JWT | Submits all uploaded documents for admin review |

### 3. Notifications
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `GET /api/v1/notifications` | list | JWT | Returns paginated notifications for authenticated user, newest first |
| `GET /api/v1/notifications/unread-count` | unreadCount | JWT | Returns number of unread notifications for badge indicator |
| `PUT /api/v1/notifications/read-all` | markAllAsRead | JWT | Marks all unread notifications as read for authenticated user |

### 4. Payments / Payouts
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `POST /api/v1/webhooks/lipila/collections` | lipilaCollectionCallback | None (shared secret) | Receives mobile-money collection status updates from Lipila |
| `POST /api/v1/wallets/{walletId}/payouts` | payout | JWT | Pays out to mobile-money account; supports Idempotency-Key header; subject to PIN, velocity, daily-send limits |

### 5. Transactions
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `POST /api/v1/wallets/{walletId}/withdraw` | withdraw | JWT | Withdraw money from wallet (decreases balance). Requires PIN. |
| `POST /api/v1/transfers` | transfer | JWT | Transfer money to another user's wallet. Requires PIN. |
| `GET /api/v1/wallets/{walletId}/transactions` | list | JWT | Get paginated transaction history. Supports filtering by type, status, date range. |
| `GET /api/v1/transactions/{reference}` | getByReference | JWT | Lookup single transaction by human-readable reference |
| `GET /api/v1/wallets/{walletId}/balance` | getBalance | JWT | Get current balance and currency for wallet |
| `POST /api/v1/wallets/{walletId}/deposit` | deposit | JWT | Direct deposits temporarily disabled; never credits wallet |

### 6. Wallet
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `GET /api/v1/wallets/me` | getMyWallet | JWT | Returns authenticated user's wallet with balance and status |
| `GET /api/v1/wallets/{walletId}/statements` | getStatement | JWT | Returns summary of all transactions for given month (YYYY-MM) |

### 7. Fees
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `GET /api/v1/fees/estimate` | estimateFee | JWT | Calculate estimated fee for transaction type and amount |
| `GET /api/v1/admin/fees` | listFees | ADMIN | Returns all fee configurations |
| `GET /api/v1/admin/fees/{id}` | getFee | ADMIN | Returns specific fee configuration by ID |
| `PUT /api/v1/admin/fees/{id}` | updateFee | ADMIN | Updates fee configuration |
| `POST /api/v1/admin/fees` | createFee | ADMIN | Creates new fee configuration |
| `DELETE /api/v1/admin/fees/{id}` | deleteFee | ADMIN | Deletes fee configuration by ID |

### 8. Admin (internal only)
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `GET /api/v1/admin/wallets` | listWallets | ADMIN | Returns all wallets with optional status filter |
| `GET /api/v1/admin/transactions` | searchTransactions | ADMIN | Search transactions by reference, type, status, date range, wallet |
| `GET /api/v1/admin/audit-logs` | searchAuditLogs | ADMIN | Search audit logs by entity type, action, admin, date range |
| `POST /api/v1/admin/wallets/{walletId}/freeze` | freezeWallet | ADMIN | Freeze wallet to prevent all transactions |
| `POST /api/v1/admin/wallets/{walletId}/unfreeze` | unfreezeWallet | ADMIN | Restore frozen wallet to active status |
| `GET /api/v1/admin/kyc/pending` | listPendingKyc | ADMIN | Returns users whose KYC is pending review |
| `GET /api/v1/admin/kyc/{userId}` | getUserKycDetail | ADMIN | Returns user's KYC status, documents, and tier |
| `GET /api/v1/admin/kyc/{userId}/documents/{documentId}` | downloadKycDocument | ADMIN | Download KYC document file |
| `POST /api/v1/admin/kyc/{userId}/approve` | approveKyc | ADMIN | Approve KYC submission and assign tier (creates wallet) |
| `POST /api/v1/admin/kyc/{userId}/reject` | rejectKyc | ADMIN | Reject KYC submission with reason |
| `GET /api/v1/admin/users` | listUsers | ADMIN | Returns all registered users |
| `GET /api/v1/admin/users/{userId}` | getUserById | ADMIN | Returns specific user details by UUID |
| `PATCH /api/v1/admin/users/{userId}/deactivate` | toggleUserStatus | ADMIN | Soft-deactivates or restores user account |

### 9. Webhooks (inbound only)
| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `POST /api/v1/webhooks/lipila/collections` | callback | Shared secret (X-Webhook-Secret) | Inbound Lipila payment callbacks |

---
## Phase 1D Coverage Analysis

**Phase 1D Requirements** (API contract the Flutter app will integrate against):

| Domain | Requirement | Status | Notes |
|---|---|---|---|
| **Identity** | OTP send/verify | **Missing** | Only email verification exists (POST /api/v1/email/verify). No OTP SMS/ push OTP endpoint. |
| | phone/email login | **Implemented** | POST /api/v1/auth/login |
| | signup token/registration | **Implemented** | POST /api/v1/auth/register |
| | PIN set/verify | **Partial** | PIN set exists (POST /api/v1/pin), but PIN verify is implicit in wallet ops (withdraw/transfer). No dedicated PIN verify endpoint. |
| | device trust | **Missing** | No device trust/registration endpoints found. |
| | device verification | **Missing** | No device verification endpoints found. |
| | device listing/removal | **Missing** | No device listing/removal endpoints found. |
| | logout | **Partial** | AuthService.logout() exists but not exposed as REST endpoint in AuthController. |
| **App config** | GET /api/v1/app/config | **Missing** | No app config endpoint found. |
| | GET /api/v1/app/notices | **Missing** | No app notices endpoint found. |
| **KYC** | tiers | **Partial** | KYC status endpoint returns tier info, but no dedicated tier listing endpoint. |
| | document types | **Implemented** | POST /api/v1/kyc/documents/upload supports NATIONAL_ID, PASSPORT, SELFIE, PROOF_OF_ADDRESS |
| | rejection reasons | **Missing** | No endpoint to fetch rejection reasons. Admin reject exists but no API for client to see available reasons. |
| | document upload | **Implemented** | POST /api/v1/kyc/documents/upload |
| | verification status | **Implemented** | GET /api/v1/kyc/status |
| **Notifications** | list | **Implemented** | GET /api/v1/notifications |
| | mark read | **Partial** | PUT /api/v1/notifications/read-all marks ALL as read; no "mark single as read" endpoint. |
| | push-token registration | **Missing** | No push-token registration endpoint found. |
| | preferences | **Missing** | No preferences endpoint found. |
| **Payments** | mobile-money collections | **Partial** | Webhook POST /api/v1/webhooks/lipila/collections exists (inbound only). No client-initiated collection endpoint. |
| | payment methods | **Missing** | No payment methods endpoint found. |
| | payment account linking | **Missing** | No payment account linking endpoint found. |
| | payout name check | **Partial** | Integrated into PayoutController; no separate name-check endpoint. |
| | user lookup | **Missing** | No user lookup endpoint found. |
| | contact matching | **Missing** | No contact matching endpoint found. |
| | payment requests | **Missing** | No payment requests endpoint found. |
| | splits | **Missing** | No splits endpoint found. |
| | QR generation/resolution | **Missing** | No QR generation/resolution endpoints found. |
| **Transactions** | search/filters | **Implemented** | GET /api/v1/wallets/{walletId}/transactions with type, status, date range filters |
| | categories | **Missing** | No categories endpoint found. |
| | statements | **Implemented** | GET /api/v1/wallets/{walletId}/statements |
| | PDF receipts | **Missing** | No PDF receipt endpoint found. |
| | support cases | **Missing** | No support cases endpoint found. |
| **Bills/store** | billers | **Missing** | No billers endpoint found. |
| | bill validation | **Missing** | No bill validation endpoint found. |
| | bill payment | **Missing** | No bill payment endpoint found. |
| | airtime | **Missing** | No airtime endpoint found. |
| | data bundles | **Missing** | No data bundles endpoint found. |
| | saved billers | **Missing** | No saved billers endpoint found. |
| | digital products/orders | **Missing** | No digital products/orders endpoint found. |
| **Savings** | budgets | **Missing** | No budgets endpoint found. |
| | goals | **Missing** | No goals endpoint found. |
| | goal deposits/withdrawals | **Missing** | No goal deposit/withdrawal endpoint found. |
| | round-ups | **Missing** | No round-ups endpoint found. |
| | cash entries | **Missing** | No cash entries endpoint found. |
| | Chilimba tracker | **Missing** | No Chilimba tracker endpoint found. |
| **Cards** | waitlist only (live cards disabled) | **Missing** | No card endpoints found. Waitlist/not implemented. |
| **AI** | monthly summaries | **Missing** | No monthly summaries endpoint found. |
| | tips | **Missing** | No tips endpoint found. |
| | read-only chat | **Missing** | No chat endpoint found. |
| | consent management | **Missing** | No consent management endpoint found. |
| **Privacy/support** | support messages | **Missing** | No support messages endpoint found. |
| | privacy consent | **Missing** | No privacy consent endpoint found. |
| | data export | **Missing** | No data export endpoint found. |
| | referrals | **Missing** | No referrals endpoint found. |
| | account closure checks | **Missing** | No account closure check endpoint found. |
| | help articles | **Missing** | No help articles endpoint found. |

---
## Contract Inconsistencies — Payout Flow

| Issue | Description | Impact |
|---|---|---|
| **Idempotency-key handling** | `POST /api/v1/wallets/{walletId}/payouts` accepts `Idempotency-Key` header. The `PayoutService` checks for `[IDEMPOTENCY:<key>]` marker in transaction description. However, the `IdempotencyFilter` runs *before* the controller, and if a cached 2xx response exists, the filter returns it directly without reaching the idempotency check in service layer. | Potential duplicate payouts if filter cache key differs from service key |
| **Payout endpoint shape** | `POST /api/v1/wallets/{walletId}/payouts` returns 201 (Successful), 202 (Pending), or 400 (Failed). The response body contains `gatewayStatus` and `gatewayMessage`. No standardized `status` enum field in the response body for client consumption; relies on `gatewayStatus` string. | Client must parse gatewayStatus string rather than using a typed enum |
| **Status enum consistency** | Transaction statuses used across the codebase: `TransactionStatus` enum (PENDING, SUCCESSFUL, FAILED, PENDING_RECONCILIATION), Payout gateway status strings ("Successful", "Failed", "Pending"). These are not aligned — payout flow uses gateway status strings while transactions use the domain enum. | Inconsistent status modeling across domains; Flutter app may need to map both |
| **Collection vs Payout symmetry** | Inbound collections arrive via webhook `POST /api/v1/webhooks/lipila/collections` with no corresponding client-initiated "collection" endpoint. The payout flow is outbound-only from wallet. | Asymmetric API contract: Flutter app can initiate payouts but not collections via API (only via webhook callback) |
| **PIN requirement discrepancy** | Payout documentation says "Requires PIN" but the `SecurityConfig` permits-all paths do not include `/api/v1/wallets/{walletId}/payouts`. The PIN check happens in `PayoutService.requirePaymentsEnabled()` and `authService.verifyPin()`. | Documentation vs implementation mismatch; Flutter app may fail if PIN not sent |

---
## Highest-Priority Gaps for Flutter Batch 1/2 Integration

| Priority | Gap | Reason | Suggested Resolution |
|---|---|---|---|
| **P0** | **Identity — OTP send/verify** | Flutter app needs OTP-based verification for login/registration flow. Only email verification token exists; no OTP over SMS/push. | Add `POST /api/v1/otp/send` and `POST /api/v1/otp/verify` endpoints, or extend email verification to support SMS OTP. |
| **P0** | **Identity — logout** | AuthService.logout() exists but is not a REST endpoint. Flutter session management requires a logout API. | Expose `POST /api/v1/auth/logout` in AuthController, calling authService.logout(userId). |
| **P0** | **App config — GET /api/v1/app/config** | Flutter app needs app configuration (features, toggles, endpoints). Currently no endpoint; app would need hardcoded values. | Add `GET /api/v1/app/config` returning app version, feature flags, supported payment methods, etc. |
| **P0** | **Notifications — push-token registration** | Flutter app needs to register push notification tokens. No endpoint exists; notifications would fail to deliver. | Add `POST /api/v1/notifications/reg-token` to register FCM/Expo push token. |
| **P1** | **KYC — rejection reasons** | Flutter KYC flow needs to display available rejection reasons if upload is rejected. No API to fetch reasons. | Add endpoint to list rejection reasons, or include in KYC status response. |
| **P1** | **Payments — payment methods** | Flutter app needs to display available payment methods before initiating payout. No endpoint. | Add `GET /api/v1/payment-methods` listing supported mobile-money providers. |
| **P1** | **Transactions — categories** | Flutter transaction filter UI needs categories for filtering. No categories endpoint exists. | Add `GET /api/v1/transaction-categories` or include in transaction history response. |
| **P1** | **Payments — QR generation/resolution** | Flutter Batch 2 may need QR for cash-in flows. No QR endpoint exists. | Add `GET /api/v1/qr/{reference}` or payment request endpoint with QR data. |
| **P2** | **Bills/store — billers, airtime, data bundles** | Core payment flow for Flutter Batch 2. Currently no bill payment infrastructure. | Implement biller catalog, bill validation, airtime/data purchase endpoints. |
| **P2** | **Savings — goals, budgets** | Savings/goal-setting is a key Flutter engagement feature. No endpoints exist. | Implement goals/budgets endpoints with deposit/withdrawal operations. |
| **P2** | **Cards — waitlist/live status** | Card management may be needed for future phases. Currently no card endpoints; live cards must remain disabled per architecture. | If needed: add card waitlist endpoint; otherwise mark as out of scope for Batch 1/2. |
| **P3** | **Privacy/support — data export, referrals, help articles** | Support and privacy features for Flutter app. No endpoints exist. | Add data export endpoint, referral code API, and help articles listing. |

**Summary**: 5 P0 gaps, 7 P1 gaps, 5 P2 gaps, 3 P3 gaps. P0 items (OTP, logout, app config, push-token) are blocking Flutter Batch 1 integration. P1 items (payment methods, categories, KYC reasons) are needed for Batch 2. P2 items (bills, savings, cards) can be deferred to Batch 3.