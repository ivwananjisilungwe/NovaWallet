# NovaWallet — Full-Stack Integration Scan Report

> Generated: 2026-08-06 · Method: static cross-check of `novawallet-api` (Spring Boot 3.5.3 / Java 17 / PostgreSQL / Flyway) against `novawallet-app` (Flutter / Riverpod / go_router / Dio). No code was run or modified.
> Companion docs: `SCAN-REPORT.md` (backend audit) · `APP-SCREEN-COVERAGE-REPORT.md` (design coverage)

## 1. Architecture at a glance

| Layer | Backend | Flutter |
|---|---|---|
| Transport | REST `/api/v1`, Bearer JWT, `ApiResponse<T>` envelope | Dio + `ApiInterceptor`, envelope unwrap |
| Auth | JWT (15 min) + rotating refresh token (7 days, SHA-256 stored, single-use, stolen-token detection) | Single-flight refresh-on-401, tokens in `flutter_secure_storage` |
| Idempotency | `Idempotency-Key` header, 24h TTL, atomic insert, replay returns cached response | Same header on all POST/PUT/PATCH/DELETE |
| Rate limit | Global 100/min, auth 10/min, login 5 fails/15min lockout, PIN 3 fails/15min | `ApiException.isRateLimited` (no retry-after handling) |
| Money | Pessimistic locking, atomic balance updates, KYC tier limits, fee engine (HALF_EVEN, min/max clamp) | Repository pattern, `AsyncValue` balance |
| Tests | 29 classes / 123 tests (concurrency, idempotency, rate-limit, refresh-rotation) | 1 boilerplate `widget_test.dart` |

**Bottom line:** the backend is healthy and well-tested; the Flutter app is a faithful design implementation that has **never been run against the real backend**. The contract cross-check found 7 broken integration flows (all admin + KYC paths) and 2 release-blocking Android config gaps.

## 2. Contract verification matrix

| Flutter call | Backend endpoint | Verdict |
|---|---|---|
| `POST /v1/auth/register` | `POST /api/v1/auth/register` (201) | ✅ match |
| `POST /v1/auth/login` | `POST /api/v1/auth/login` (200 / 401 / 429) | ✅ match |
| `POST /v1/auth/refresh` — refresh token in `Authorization: Bearer` | `POST /api/v1/auth/refresh` reads `@RequestHeader("Authorization")` | ✅ match |
| `POST /v1/password/forgot` `{email}` | `POST /api/v1/password/forgot` | ✅ match |
| `POST /v1/password/reset` `{token, newPassword}` | `POST /api/v1/password/reset` | ✅ match |
| `POST /v1/email/verify?token=` | `POST /api/v1/email/verify?token=` | ✅ match |
| `POST /v1/pin` `{pin}` | `POST /api/v1/pin` | ✅ match |
| `GET / PUT /v1/users/me` | `GET / PUT /api/v1/users/me` | ✅ match |
| `GET /v1/wallets/me` | `GET /api/v1/wallets/me` | ✅ match |
| `GET /v1/wallets/{id}/balance` | `GET /api/v1/wallets/{id}/balance` | ✅ match |
| `GET /v1/wallets/{id}/transactions` | `GET /api/v1/wallets/{id}/transactions` | ✅ match (no `page`/`size` sent) |
| `GET /v1/transactions/{reference}` | `GET /api/v1/transactions/{reference}` | ✅ match |
| `POST /v1/wallets/{id}/deposit` | `POST /api/v1/wallets/{id}/deposit` (201) | ⚠️ response parsed as `Wallet`, backend returns `TransactionResponse` |
| `POST /v1/wallets/{id}/withdraw` | `POST /api/v1/wallets/{id}/withdraw` (201) | ⚠️ same |
| `POST /v1/transfers` | `POST /api/v1/transfers` (201) | ⚠️ same |
| `GET /v1/fees/estimate?type=&amount=` | `GET /api/v1/fees/estimate` | ✅ match (enum values align) |
| `GET /v1/kyc/status` | `GET /api/v1/kyc/status` | ✅ match |
| `POST /v1/kyc/documents/upload` (multipart) | `POST /api/v1/kyc/documents/upload` | ⚠️ stray `path` query param ignored; **never called by any screen** |
| `POST /v1/kyc/submit` | `POST /api/v1/kyc/submit` | ⚠️ path matches, but no documents are ever uploaded → backend 400 |
| `GET /v1/admin/kyc/pending` | `GET /api/v1/admin/kyc/pending` | ❌ backend returns bare `List`, app parses `PagedResponse.content` → **queue always empty** |
| `POST /v1/admin/kyc/{id}/approve` | `POST /api/v1/admin/kyc/{id}/approve` | ❌ backend requires `{tier}` body, app sends none → **400** |
| `POST /v1/admin/kyc/{id}/reject` | `POST /api/v1/admin/kyc/{id}/reject` | ❌ backend requires `{reason}` body, app sends none → **400** |

Envelope (`success/data/message/timestamp`), pagination (`content/page/size/totalElements/totalPages`), enum names, ISO-8601 dates, and the `Idempotency-Key` contract all match. Flutter models use `.toString()` on amounts, so the backend's BigDecimal-as-number serialization is safe.

## 3. Findings by severity

### 🔴 P0 — Release-blocking

1. **Android release builds have no network permission.** `INTERNET` is declared only in `android/app/src/debug/AndroidManifest.xml` and `src/profile/` — the main manifest has none. Release builds cannot make any HTTP call.
2. **Android cleartext HTTP is blocked.** Base URL is `http://10.0.2.2:8080/api` (emulator) / `http://localhost:8080/api`; no `android:usesCleartextTraffic` and no network-security-config. On API 28+ the app cannot connect even in debug. (iOS is unaffected — ATS exempts loopback.)
3. **Admin KYC queue is always empty** — bare `List<UserSummaryResponse>` parsed as `PagedResponse` (`content` key) → admin console shows "Queue clear" regardless of backend state.
4. **Admin approve/reject always fail** — `ApproveKycRequest` (`tier`, `@NotNull @Min(1)`) and `RejectKycRequest` (`reason`, `@NotBlank`) are required bodies; the app sends none → 400 `VALIDATION_ERROR`.
5. **KYC submission always fails.** `KycUploadScreen` never calls `uploadDocument()` (no file picker; `image_picker` not a dependency), and `submitKyc()` posts an empty body → backend rejects with "No documents uploaded". The selected document type is local-only, and `DRIVERS_LICENSE` is not a backend enum (`NATIONAL_ID, PASSPORT, SELFIE, PROOF_OF_ADDRESS`).

### 🟠 P1 — High

6. **Money-movement endpoints parse the wrong response type.** `WalletRepository.deposit/withdraw/transfer` parse `TransactionResponse` as `Wallet`: `balance → '0'`, `status → 'SUCCESSFUL'`, `accountNumber → ''`. Screens currently discard the result and call `refreshBalance()`, so it is not user-visible — but the contract is wrong and any future use of the return value silently misbehaves. Fix: return `TransactionResponse` (or void + refresh).
7. **New users have no wallet.** Backend creates wallets only on KYC approval; `GET /wallets/me` → 404 → dashboard error state. The app needs a "complete KYC to activate your wallet" state instead of an error.
8. **`ChangePasswordScreen` is broken.** It calls `resetPassword(token: '', …)`; the backend requires a non-blank token. There is no authenticated change-password endpoint on the backend at all.
9. **`auth_pin_set` survives logout.** `TokenStorage.clear()` deletes access/refresh/user JSON but not the PIN flag, so the PIN-set gate is skipped for the next account on the same device.
10. **No admin route guard.** `/admin` is registered for all authenticated users; only the profile tile is hidden. A non-admin can navigate directly to `/admin` (403 at the API layer).
11. **No pagination.** History and admin queue never send `page`/`size`; only the first 20 records are ever visible.

### 🟡 P2 — Medium

12. **Registration hardcodes `phone: '+260700000000'`** and derives names from the email local-part. The backend has a unique index on phone → the second registration on the same device fails with 409.
13. **Static screens:** Cards, Notifications (3 hardcoded items), Statements, Security (PIN change "coming soon"), dashboard quick actions (Request/Airtime/Bills) disabled.
14. **429/409 bodies are not `ApiResponse`-wrapped** (raw `ErrorResponse` shape). The client still surfaces `message`, but the `code` field is dropped.
15. **Backend notes:** email-verification token has no TTL enforcement (email copy says 24h); no logout endpoint (refresh tokens live 7 days; app logout is client-only — acceptable); `GET /fees/estimate` is `permitAll` despite Swagger docs saying authenticated.
16. **`User` model dead fields.** `kycStatus`/`kycTier`/`wallet` are parsed but never sent in `AuthResponse` (backend `UserInfo` has only id/names/email/phone/role/emailVerified/pinSet) — masks shape drift.

## 4. What's solid (do not touch)

- Refresh-token rotation with single-flight detection — the Bearer-header refresh contract matches the backend exactly.
- Idempotency-Key on both sides; envelope + pagination + error shapes; enum names; fee-engine contract.
- Backend: pessimistic locking, atomic balance updates, KYC tier limits, audit aspect, input sanitization, rate limiting, 123 green tests.
- Flutter: secure token storage, typed models, loading/empty/error states, pull-to-refresh.

## 5. Recommended priority order

1. **Android manifest** — add `INTERNET` to the main manifest; allow cleartext for debug (or network-security-config); require `https` via `--dart-define=API_BASE_URL` for release.
2. **Admin** — parse `pendingKyc` as a bare list; send `{tier}` on approve and `{reason}` on reject.
3. **KYC** — wire `uploadDocument` (add `image_picker`), send the real `documentType`, then `submitKyc`.
4. **Wallet** — return `TransactionResponse` from money-move calls; add a no-wallet → KYC prompt state.
5. **Auth** — clear `auth_pin_set` on logout; add an authenticated change-password endpoint + screen; collect phone on registration.
6. **Pagination** on history + admin queue; **admin route guard**.