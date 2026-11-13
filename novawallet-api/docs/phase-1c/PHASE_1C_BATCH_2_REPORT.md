# NovaWallet — Phase 1C Batch 2 Report

**Document:** `PHASE_1C_BATCH_2_REPORT.md`
**Phase:** 1C — Security and Database Hardening
**Batch:** 2 — Authorization and Ownership (prompt BE-1C-2)
**Branch:** `feature/be-1c-2-authz` (per `git status` at report time; the Batch 1 report names `feature/phase-1c-security-database-hardening`)
**Plan:** `docs/phase-1c/PHASE_1C_BATCH_2_PLAN.md` (findings F1–F9)
**Reported:** 2026-11-14
**Status:** COMPLETE — awaiting independent review (T1, `prompts/templates/T1_independent_review.md`). No commit, push, merge, rebase or deployment performed by this report.

---

## A. Batch 2 decision

**COMPLETE, pending independent review.**

In-scope items addressed:

- SEC-10 — input bounds (partial; see Sections H and M).
- IDOR and ownership verification on walletId, reference, payout and KYC document routes (no service code change needed; proven by tests).
- Route-level authentication enumeration (every non-public route returns 401 when anonymous).
- Admin URL-level protection (defence in depth on top of existing `@PreAuthorize`).
- Mass-assignment review (unknown JSON fields rejected).
- Role-separation assessment: assessed only. DEC-03 keeps a single `ADMIN` role; split roles are deferred to BE-1D-07. The single-ADMIN risk is recorded in the canonical register.

Not claimed: production readiness, Java 17 runtime compatibility, or PostgreSQL behaviour for this batch (no migration was added).

---

## B. Decision record

| Decision | Resolution | Implementation |
|---|---|---|
| DEC-03 | Single `ADMIN` role kept for now; split roles in BE-1D-07 | No role model change. SEC-13 single-ADMIN risk recorded in `docs/architecture/backend-risk-register.md` (commit `fac6f61`). |
| F1 | Ownership already enforced in services | No production change; proving tests added. |
| F3 | No URL-level admin rule | `SecurityConfig` adds `/api/v1/admin/**` → `hasRole("ADMIN")` before `anyRequest()`. |
| F2, F4–F8 | Bounds and status mapping | See Sections D, F, H and I. |
| F5, F9 | Unknown JSON fields rejected | See Section G. |

No new DEC entry was created by this batch.

---

## C. Ownership, IDOR and route authentication (F1, route enumeration)

Ownership checks (code unchanged, now proven by tests):

- `TransactionHistoryService` — `verifyWalletOwnership` for statements, transaction lists and balance reads.
- `TransactionService.findActiveWalletWithLock` — deposit and withdraw.
- `PayoutService.findOwnedActiveWalletWithLock` — payout initiation.
- `TransactionHistoryService.getTransactionByReference` — caller must own sender or receiver wallet.
- `AdminKycService` — admin document download requires the document to belong to the path `userId`.

Tests:

- `endpoint/AuthorizationMatrixIntegrationTest` — `owner_canRead`, `intruder_cannotRead`, `intruder_cannotMoveMoney`, `unknownAndMalformedIds`, `unauthenticated`, `referenceOwnership`, `customerGets403_anonymousGets401` (parameterised over every admin route), `adminAllowed`.
- `endpoint/PayoutEnabledIntegrationTest#payout_fromAnotherUsersWallet_isForbidden_andGatewayNotCalled` — non-owner payout is 403 and the payment gateway is not called.
- `endpoint/RouteSecurityEnumerationTest#everyNonPublicRouteRequiresAuthentication` — enumerates handler mappings; anonymous requests to every non-public route return 401. The allow-list matches the `permitAll` rules in `SecurityConfig`.

Test bug fixed during the batch: `AuthorizationMatrixIntegrationTest` used `TransactionStatus.COMPLETED`, which is not a valid value; corrected to `TransactionStatus.SUCCESSFUL`. No assertion was weakened.

Authorization map: `reports/PHASE_1C_AUTHZ_MAP.md` (48 routes). The six rows that had no behavioural test are now covered by `AuthorizationGapIntegrationTest` (C.1).

### C.1 Gap tests

`endpoint/AuthorizationGapIntegrationTest` (5 tests):

- `notifications_areScopedPerUser` — user B cannot see, count or mark-all-read user A's notifications. Asserts against per-user baselines because registration creates each user's own notifications.
- `pinChange_anonymous_isUnauthorized` — `POST /api/v1/pin/change` without a token → 401.
- `pinChange_invalidBody_isBadRequest` — authenticated invalid PIN body → 400.
- `kycUpload_anonymous_isUnauthorized` — anonymous multipart upload → 401.
- `adminDocumentDownload_crossUserDocument_isForbidden` — admin requests a document id owned by another user → 403, and the response does not contain the file content.

---

## D. Paging bounds (F2)

- New helper `config/PageRequests.of(page, size[, sort])`: `page >= 0`, `1 <= size <= 100`; otherwise 400.
- Applied at `TransactionController`, `NotificationController`, `AdminUserController` and `AdminWalletController` (three call sites).
- Before: `size=10000000` was accepted.

Evidence: `InputBoundsIntegrationTest` — `sizeAboveMaximum_isBadRequest` (101 → 400), `sizeZero_isBadRequest` (0 → 400), `negativePage_isBadRequest` (-1 → 400), `sizeAtMaximum_isOk` (100 → 200).

---

## E. Admin URL-level protection (F3)

- `SecurityConfig`: `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` before `anyRequest()`.
- Method security (`@EnableMethodSecurity`, class-level `@PreAuthorize("hasRole('ADMIN')")` on the admin controllers) was already active and is unchanged.
- Result: an admin route is protected by two independent layers.

Evidence: `AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401` (parameterised, all admin routes); `AuthorizationMatrixIntegrationTest#adminAllowed`.

Admin routes 25, 26, 46, 47 and 48 in the authz map have denial coverage only (no admin-success test). Unchanged.

---

## F. Multipart upload size (F4)

- `application.yml`: `spring.servlet.multipart.max-file-size: 10MB`, `max-request-size: 11MB`. The KYC service limit is 10 MB; Spring's 1 MB default was below it.
- `GlobalExceptionHandler`: `MaxUploadSizeExceededException` → HTTP 413, error code `PAYLOAD_TOO_LARGE`.

Evidence: `InputBoundsIntegrationTest#configuredLimitAndHandlerMapping` asserts the configured property and the handler mapping.

**Limitation:** MockMvc does not enforce the servlet multipart limit. The 413 response is therefore **not** proven end-to-end. An end-to-end check against a real servlet container is still required.

---

## G. Unknown fields and mass assignment (F5, F9)

- `spring.jackson.deserialization.fail-on-unknown-properties: true`.
- `@JsonIgnoreProperties(ignoreUnknown = true)` added to `GatewayStatusResponse` and `GatewayInitiationResponse` (provider response DTOs). `GatewayCallbackPayload` already had it.
- No request DTO binds `id`, `userId`, `role`, `balance` or `status` (F9 review).

Evidence: `InputBoundsIntegrationTest#updateProfile_roleField_isBadRequest` (`PUT /users/me` with `role=ADMIN` → 400; role unchanged) and `#transfer_balanceField_isBadRequest` (transfer with `balance=1` → 400).

**Risk:** the global Jackson setting applies to every Boot-managed `ObjectMapper`. Inbound provider payloads (for example webhook bodies) and any other external JSON parsed with that mapper now fail on unknown fields. The full suite passes, but this batch did not individually audit every inbound and outbound DTO. See Section M.

---

## H. DTO bounds and admin inputs (F6, F7, F8)

| Input | Bound | Test |
|---|---|---|
| Deposit / Withdraw / Transfer / Payout `amount` | `@Digits(integer=13, fraction=2)` and `@DecimalMax("9999999999999.99")` (already in source at batch start; executable evidence added here) | `transfer_subMinorUnitAmount_isBadRequest` (0.001), `transfer_amountAboveMaximum_isBadRequest`, `withdraw_subMinorUnitAmount_isBadRequest`, `withdraw_amountAboveMaximum_isBadRequest` |
| Transaction PIN (Transfer / Withdraw) | `@Pattern ^\d{4,6}$` (added to Transfer, Withdraw and Payout DTOs) | `transfer_nonDigitPin_isBadRequest`, `withdraw_nonDigitPin_isBadRequest` |
| Payout `accountNumber` | `@Pattern ^\+?\d{9,15}$` | `payout_nonNumericMsisdn_isBadRequest` |
| Login / Forgot / Register email | `@Size(max=254)` | Full suite only (no dedicated boundary test) |
| Login password | max 128 | Full suite only |
| Reset token | max 256 | Full suite only |
| Change-password current password | max 128 | Full suite only |
| KYC reject reason | max 500 | Full suite only |
| CreateFee | type max 20; percentage ≤ 1.0000 (`@Digits(1,4)`); flat / min / max `@Digits(10,2)` ≥ 0 | `createFee_percentageAboveOne_isBadRequest` (1.5 → 400) |
| `GET /fees/estimate` `amount` (F7) | scale ≤ 2 and ≤ 13 integer digits; else 400 | `feeEstimate_threeDecimalPlaces_isBadRequest` (1.001 → 400) |
| KYC approve with unknown tier (F8) | `IllegalArgumentException` → `BadRequestException` (400) | `approveKyc_unknownTier_isBadRequest` (tier 99 → 400; submission stays PENDING) |

Notes:

- The amount bounds were already present in DTO source at batch start. This batch added executable evidence.
- `FeeEstimateRequest` is unused (the endpoint takes request parameters). Validation lives in `FeeController`.
- The narration / description length bound from SEC-10 is **not** addressed by this batch.

---

## I. HTTP status contract changes

| Condition | Before | After |
|---|---|---|
| `size` > 100, `size` < 1, `page` < 0 | 200 (unbounded) | 400 |
| Upload above configured multipart limit | 500 (generic handler) | 413 `PAYLOAD_TOO_LARGE` (not proven end-to-end; see F) |
| Unknown JSON field in request body | 200 (ignored) | 400 |
| Fee estimate amount with >2 decimals or >13 integer digits | 200 | 400 |
| KYC approve with unknown tier | 500 | 400 |
| Non-owner access to walletId / reference / payout / document | 403 (unchanged) | 403 (unchanged, now proven) |
| Anonymous access to protected routes | 401 (unchanged) | 401 (unchanged, now proven by enumeration) |

Authentication failures remain 401 and authorization failures remain 403, as set in Batch 1 Section I.

---

## J. Regression results

Evidence (JDK 21 runtime):

- Baseline before batch: **316 tests**.
- Full suite: `cd novawallet-api && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test` — exit **0**, **Tests run: 361, Failures: 0, Errors: 0, Skipped: 0**.
- Targeted run, executed after the full run: `./mvnw -B test -Dtest=AuthorizationGapIntegrationTest` — **Tests run: 5, Failures: 0**.
- Combined full run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test` (2026-11-14, after the gap test was added): exit 0, **Tests run 366, Failures 0, Errors 0, Skipped 0** (observed).
- Java 17 runtime: **not verified** (BLK-04). Source/release is 17; the runtime used was JDK 21.
- No real provider request was made. Sandbox and test configuration only.

---

## K. PostgreSQL evidence

Not applicable to this batch. **No migration was added**; Flyway head remains `V16__create_revoked_access_tokens.sql`. No PostgreSQL 16 fresh or upgrade check was run in this batch.

---

## L. Git inventory

No staging, commit, push, merge, rebase or history rewrite was performed by this report.

Batch 2 commits on `feature/be-1c-2-authz`:

- `fac6f61` — records SEC-13 (single-ADMIN risk, DEC-03) in `docs/architecture/backend-risk-register.md`; fixes the enum in `AuthorizationMatrixIntegrationTest`.
- `25b4ced` — adds `endpoint/RouteSecurityEnumerationTest.java` and `endpoint/InputBoundsIntegrationTest.java`.
- `965946e` — adds `endpoint/AuthorizationGapIntegrationTest.java`; adds plans for batches 6–8.

Observation: the production source changes for F2–F8 (`config/PageRequests.java`, `config/SecurityConfig.java`, `application.yml`, `GlobalExceptionHandler`, the DTOs, `AdminKycService`, `FeeController`, the four controllers) do **not** appear as separate Batch 2 commits. `git log --stat` attributes files of that kind to the earlier monorepo import commit `6653046`. This batch did not verify that the working-tree F2–F8 changes match that commit. Independent review should diff them against `6653046`.

`git status` at report time: no uncommitted changes in `novawallet-api/` except the untracked `docs/phase-1c/PHASE_1C_BATCH_3_PLAN.md`, which this batch did not create.

---

## M. Remaining risks

Known, not fixed in this batch:

- `TransactionHistoryService.getMonthlyStatement` totals only the first 100 transactions of a month. Correctness bug, not authorization. → **BE-1D-12**.
- `GET /api/v1/transactions/{reference}` returns **403** for another customer's reference and **404** for an unknown one, which reveals that the reference exists. Changing this is an API-contract change. → **BE-99** (raise there).
- `ChangePasswordRequest.newPassword` max is 100, while register and reset allow 128. Inconsistent; left as is.
- `FeeController` Javadoc (`fee/controller/FeeController.java:29-30`) says "No authentication required". The route is authenticated (`SecurityConfig`). The Javadoc is wrong; not corrected in this batch.
- `/api/v1/webhooks/lipila/**` is `permitAll` (`SecurityConfig.java:110`). Any future route under that prefix is public by default; each handler must enforce its own secret or HMAC.
- KYC document access is streamed (`AdminKycController`), not signed expiring URLs as specified in `docs/api/CONTRACT_TARGET.md` A10. → **BE-1C-6**.
- Multipart 413 is asserted via configuration and handler mapping, **not end-to-end** (MockMvc does not enforce the servlet limit).
- Global `fail-on-unknown-properties: true` (Section G): inbound and outbound JSON DTOs were not individually audited for unknown-field tolerance beyond the two provider DTOs annotated here. Review before relying on webhook payloads with extra fields.
- SEC-10 narration length bound: not addressed. SEC-10 is **IN PROGRESS**, not closed.
- SEC-13 single-ADMIN role (DEC-03): accepted for now; split roles in **BE-1D-07**.
- Email verification token expiry (SEC-07) remains OPEN and out of scope.
- Java 17 runtime compatibility: not verified (BLK-04).

Carried from earlier batches and unchanged: DEC-04 (R-01/R-03), DEC-05, and Batch 3–8 items.

---

## N. Recommendation

Batch 2 is ready for independent review (T1). Before accepting it, the reviewer should:

1. Run the full suite once more including `AuthorizationGapIntegrationTest` and confirm the combined count.
2. Diff F2–F8 source against `6653046` (Section L).
3. Decide on an end-to-end multipart check and on the global unknown-field setting (Sections F and G).

Batch 3 should **not** begin until the reviewer approves this batch.
