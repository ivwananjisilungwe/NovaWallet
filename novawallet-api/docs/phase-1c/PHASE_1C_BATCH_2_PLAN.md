# Phase 1C — Batch 2 Plan: Authorization & Ownership Hardening

Date: 2026-11-13 · Prompt: BE-1C-2 · Decision dependency: **DEC-03 = keep single ADMIN now; split roles in BE-1D-07** (`docs/DECISIONS.md`, owner, 2026-11-13).

## Identifiers addressed
SEC-10 (input bounds), ownership/IDOR verification, mass-assignment review, role-separation assessment (assess only, per DEC-03).

## Audit findings (pre-change)
| # | Finding | Evidence | Action |
|---|---|---|---|
| F1 | Ownership is enforced in the service layer for every user-scoped `walletId`/`reference` route | `TransactionHistoryService.verifyWalletOwnership`, `TransactionService.findActiveWalletWithLock`, `PayoutService.findOwnedActiveWalletWithLock`, `getTransactionByReference` | No code change. Add an IDOR matrix test to prove it |
| F2 | No `size`/`page` bounds on any paged endpoint (`size=10000000` accepted) | `PageRequest.of(page, size…)` in Transaction, Notification, AdminUser, AdminWallet (×3) controllers | Shared `PageRequests` helper: page ≥ 0, 1 ≤ size ≤ 100, else 400 |
| F3 | Admin routes protected by `@PreAuthorize` only; no URL-level backstop | `SecurityConfig` has no `/api/v1/admin/**` rule | Add `hasRole("ADMIN")` for `/api/v1/admin/**` (defence in depth) |
| F4 | Spring's default multipart limit (1 MB) sits below the KYC 10 MB limit; oversize uploads fall through to the generic 500 handler | `KycService.MAX_FILE_SIZE`; no `spring.servlet.multipart` config; no `MaxUploadSizeExceededException` handler | `max-file-size: 10MB`, `max-request-size: 11MB`; handler → 413 |
| F5 | Unknown JSON fields silently ignored on request bodies | Boot default | `spring.jackson.deserialization.fail-on-unknown-properties: true`; provider DTOs keep `@JsonIgnoreProperties(ignoreUnknown = true)` |
| F6 | DTO bound gaps: PIN digits-only pattern missing on Transfer/Withdraw/Payout; payout MSISDN unpatterned; login/forgot email and password without max; reset token unbounded; KYC reject reason unbounded; CreateFee lacks precision/max matching `DECIMAL(5,4)`/`DECIMAL(12,2)`/`VARCHAR(20)` | DTO sources, V7 | Add constraints |
| F7 | `GET /fees/estimate` amount unbounded | `FeeController` | Validate range/precision → 400 |
| F8 | Approving KYC with a non-existent tier returns 500 (`IllegalArgumentException`) | `AdminKycService.approveKyc` → `KycConfig.getTier` | Map to 400 |
| F9 | Mass assignment: no request DTO binds `id`, `userId`, `role`, `balance`, `status` | DTO review | Test that an extra `role`/`balance` field is rejected (F5) |

## Files to change
Plan §7 Batch 2 list, with deviations:
- Ownership resolution is **already** in the services (F1), so the controllers listed for ownership don't change.
- Added: `SecurityConfig`, `GlobalExceptionHandler`, `application.yml`, `AdminKycService`, `AdminUserController`, `AdminWalletController`, `NotificationController`, the new `config/PageRequests`, auth/user/kyc DTOs, and `GatewayInitiationResponse`/`GatewayStatusResponse` (ignoreUnknown).
- `FeeEstimateRequest` is unused (the endpoint takes request params), so validation goes in `FeeController`.

## Tests to add
- `AuthorizationMatrixIntegrationTest`: owner vs other customer vs unauthenticated, for every id-taking route; customer → every admin route is 403.
- `RouteSecurityEnumerationTest`: enumerates all handler mappings; fails if an unauthenticated route is not allow-listed.
- `InputBoundsIntegrationTest`: boundary values for amounts, PINs, MSISDN, paging, unknown fields, fee and KYC admin inputs, and oversize multipart.

## Out of scope / risks noted
- Monthly statement totals use only the first 100 transactions (`getMonthlyStatement`). This is a correctness bug, not authz; reported for BE-1D-12.
- `GET /transactions/{reference}` returns 403 vs 404, which reveals whether a reference exists. Reported only; changing the status is an API-contract change.
