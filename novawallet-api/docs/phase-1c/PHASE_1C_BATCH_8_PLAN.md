# Phase 1C Batch 8 Plan: Audit Coverage, Error Envelope & Log Redaction

Date: 2026-11-14 · Prompt: BE-1C-8 · Needs: BE-1C-7 (`PHASE_1C_BATCH_7_PLAN.md`) · Status: **PLAN ONLY — no source changed, no build run, no git writes**
Decision dependency: no DEC is recorded for audit failure policy, PII-read auditing, or log masking. Those are proposed in §7 and block implementation of those items.

## 0. Identifiers addressed
SEC-12 (request-id half), SEC-11 (headers, CORS, actuator, Swagger), SEC-04 (default `dev` profile, still open in the register), plus audit completeness (BE-1C-8 §5), security event logging (§6), and log hygiene (§2). Related: SEC-03 residual (PII in logs), BE-1C-2 F8 (unknown KYC tier returns 500).

## 1. What I read (with paths)
- Governance: root and `novawallet-api/AGENTS.md` (rules 5 "Security", 7 "API contract", 8 "Audit"), `docs/DECISIONS.md` (DEC-06 audit immutability), `T5_plan_first.md`, `_COMMON_BATCH_RULES.md`, `BE-1C-8_audit_logging_errors.md`.
- Style model: `PHASE_1C_BATCH_2_PLAN.md`. Batch 7 plan for the ordering dependency.
- Risk register: `novawallet-api/docs/architecture/backend-risk-register.md` SEC-11 (line 407), SEC-12 (line 426), SEC-04 (line 290).
- Not re-read here: `DEC-06` implementation status (REVOKE on `audit_logs`), `PHASE_1C_IMPLEMENTATION_PLAN.md` §7/§13, `docs/api/ERROR_CODES.md` (absent, see E5).

Source evidence (verified by reading the files):

| # | Fact | Evidence |
|---|---|---|
| S1 | `ErrorResponse` fields: `status, code, message, timestamp, path, errors(Map)`. No `requestId`. Field errors are a map, not `fieldErrors[]` | `exception/ErrorResponse.java:10-22` |
| S2 | Global handler: `Exception.class` catch-all returns 500 `INTERNAL_SERVER_ERROR` and logs the full stack trace | `exception/GlobalExceptionHandler.java:158-176`, log at `:163` |
| S3 | Handler passes `exception.getMessage()` straight into bodies for not-found, duplicate, bad-request, unauthorized, rate-limit, forbidden, unavailable | `GlobalExceptionHandler.java:34-82`, `:120` |
| S4 | Type mismatch echoes the raw input value into the message | `GlobalExceptionHandler.java:144-146` |
| S5 | `KycConfig.getTier` throws `IllegalArgumentException("Invalid KYC tier: N")`; this reaches the catch-all as a 500 (BE-1C-2 F8) | `kyc/config/KycConfig.java:25-28` |
| S6 | Hand-built error bodies in three other places: `RateLimitFilter.java:70-74` (string-formatted), `JwtAuthFilter.java:146-154` (`ErrorResponse`, 503), `RestAuthenticationEntryPoint.java:44-52` (`ErrorResponse`, 401). None carry a request id | files as cited |
| S7 | `RequestTracingFilter` copies `X-Request-ID` verbatim into MDC and the response header. No validation. Generates a UUID only when blank | `filter/RequestTracingFilter.java:33-47`, `:56` (MDC removed) |
| S8 | `RequestTracingFilter` is `@Component` with no `@Order`. It is therefore not in the explicit security chain. Its position relative to `FilterChainProxy` is Boot default and unverified (inferred: security runs first, so 401/403 responses may lack the header) | `filter/RequestTracingFilter.java:17` |
| S9 | Prod logging: JSON via Logstash encoder plus rolling file; dev and test: console with `%X{requestId}` | `resources/logback-spring.xml:24-39`, `:43-60`, `:80-96` |
| S10 | `SensitiveDataRedactor` is applied only to mail body, mail send error, and notification read path | `MailService.java:112`, `:127`; `NotificationResponse.java:31-32` |
| S11 | Plaintext emails in logs: register, failed login, login success, verify, reset requested, reset completed | `auth/service/AuthService.java:99`, `:118`, `:124`, `:217`, `:237`, `:259` |
| S12 | Login limiter logs `email|ip` at WARN | `security/LoginRateLimiter.java:48`, `:52`, `:66` |
| S13 | Plaintext phone numbers in logs at INFO and ERROR | `notification/service/SmsService.java:57`, `:68`, `:71` |
| S14 | Plaintext recipient email on successful send | `notification/MailService.java:124` |
| S15 | Account identifier logged on Lipila initiation (field `account`; verify whether MSISDN) | `payment/LipilaGateway.java:95`, `:102` |
| S16 | KYC approval logs `walletAccount`; rejection logs free-text `reason` | `kyc/service/AdminKycService.java:147`, `:182` |
| S17 | Admin email logged at startup (dev profile only) | `bootstrap/AdminDataInitializer.java:86` |
| S18 | `JwtAuthFilter` logs the `jti` at DEBUG for revoked tokens (identifier, not a token) | `security/JwtAuthFilter.java:110` |
| S19 | Only one log-capture test exists (`MailServiceRedactionTest`). No leak scan of the main flows | `src/test/.../notification/MailServiceRedactionTest.java` (grep: only file using capture) |
| S20 | Async audit runs on the `@Async` pool with no `AsyncConfigurer`, no `AsyncUncaughtExceptionHandler`, and no `TaskDecorator` (so MDC requestId is not propagated) | `config/AsyncConfig.java:6-10` (only `@EnableAsync`); grep finds no other async config |
| S21 | Audit write is `@Async` + `REQUIRES_NEW`, returns `CompletableFuture`; the caller cannot observe a failure | `audit/service/AuditService.java:34-36`, `:65-67`, `:45-60` |
| S22 | Audit failures are swallowed by the aspect and by the payout helper | `audit/aspect/AuditedAspect.java:176-179`; `payment/service/PayoutService.java:715-722` (comment: "Audit is best-effort") |
| S23 | `AuditLog` has `ipAddress` but no request id. The 6-argument overload (no IP) is what every call site uses, so `ip_address` is null | `audit/entity/AuditLog.java:39-43`; `AuditService.java:65-76`; call sites in §5 |
| S24 | Audited today: wallet freeze and unfreeze (admin and user delete), user soft-delete and restore, withdraw, transfer debit and credit, payout initiate/success/failed/pending, payout reconciliation, webhook quarantine | §5 table |
| S25 | Not audited: KYC approve, KYC reject, KYC document download, admin fee create/update/delete, admin transaction and audit-log searches, auth events (login, logout, password change, PIN set/change/verify, reset, verify) | §5 table |
| S26 | Security events: failed login and lockout are WARN; PIN failures are WARN; revoked-token use is DEBUG (`JwtAuthFilter.java:110`); access denied (`GlobalExceptionHandler.java:85-91`) and 401 entry point (`RestAuthenticationEntryPoint.java:34-53`) log nothing; no token-reuse log found in `auth`/`token` services | files as cited |
| S27 | Headers: `X-Content-Type-Options` (`SecurityConfig.java:60`), HSTS (`:61-64`), XSS (`:65-67`), CSP split for Swagger vs API (`:72-92`). No `Referrer-Policy` anywhere in `src/main` | `config/SecurityConfig.java:58-93`; grep |
| S28 | CORS: origins from `app.cors.origins`; defaults include localhost and `capacitor://localhost` when blank; `allowedHeaders("*")` and `allowCredentials(true)` | `SecurityConfig.java:136-160` (`:143-148`, `:152-153`); `application.yml:90-91`; `application-prod.yml:142-143` (prod default includes localhost) |
| S29 | Actuator: base exposes `health`, `info`, `info.env.enabled: true`. Prod exposes `health`, `info`, `metrics`, `prometheus`. Only health and info are `permitAll`; the rest need JWT | `application.yml:37-47`; `application-prod.yml:52-61`; `SecurityConfig.java:117-120` |
| S30 | Swagger UI and API docs `permitAll` in every profile. Prod YAML has no `enabled: false` | `SecurityConfig.java:111-116`; `application-prod.yml:166-170` |
| S31 | Base `application.yml` still sets `profiles.active: dev` (SEC-04 open, no resolved update in register) | `application.yml:5-6` |
| S32 | JWT secret: no default, startup fails if missing, not Base64, or under 256 bits. Algorithm pinned. Covered by `JwtUtilTest` per register | `security/JwtUtil.java:54-74`, `:97`; `application.yml:60` |
| S33 | Admin password: no default in base or prod | `application.yml:75`; `application-prod.yml:96` |
| S34 | Migration head is V16. `audit_logs` is V8 | `db/migration` listing; `V8__create_audit_logs_table.sql` |

## 2. Audit findings (pre-change)

| # | Finding | Evidence | Action |
|---|---|---|---|
| E1 | `X-Request-ID` not validated (log injection) | S7 | Allowlist `[A-Za-z0-9-]{1,64}`, else generate |
| E2 | Request id is absent from error bodies | S1, S6 | Add `requestId` to the common envelope |
| E3 | Error envelope shape is not the BE-1C-8 contract (`code, message, requestId, fieldErrors[]`). Field errors are a map | S1 | Add `requestId`, change field errors to an array (API change, list in §4) |
| E4 | Four body writers (`GlobalExceptionHandler`, `RateLimitFilter`, `JwtAuthFilter`, `RestAuthenticationEntryPoint`) | S6 | One shared writer used by all four |
| E5 | `Exception.class` catch-all likely turns 404 (unmapped route) and 405 into 500 (inferred for Spring 6.1+ `NoResourceFoundException` and `HttpRequestMethodNotSupportedException`; confirm by test) | S2 | Map to proper status with stable codes |
| E6 | `docs/api/ERROR_CODES.md` does not exist. `docs/api/` holds `CONTRACT_TARGET.md` and `README.md` only | `docs/api` listing | Create the catalogue (in this batch, as BE-1C-8 requires) |
| E7 | Message leakage: `IllegalArgumentException` from `KycConfig.getTier` becomes 500 (BE-1C-2 F8). Type-mismatch echoes raw value. Service messages pass through | S3, S4, S5 | Map to 400 with generic messages. Do not echo input values |
| E8 | DB constraint violations (`DataIntegrityViolationException`) have no mapping. They fall to 500 (verify) and must not expose SQL | S2 | Map to 409 `CONFLICT` with generic message |
| E9 | Provider errors: `PayoutService` logs initiation failure (`:381`); the message that reaches the client depends on `BadRequestException` text (not traced in this pass) | `payment/service/PayoutService.java:381` | Trace the path; generic provider message, code mapped in catalogue |
| H1 | No `Referrer-Policy` | S27 | Add `strict-origin-when-cross-origin` (value is standard, owner to confirm) |
| H2 | CORS: prod defaults to localhost origins if `APP_CORS_ORIGINS` is unset; wildcard headers with credentials | S28 | Fail fast in prod when origins unset or contain localhost; explicit header list |
| H3 | Actuator: `info.env.enabled: true` in base profile; prod exposes `prometheus` and `metrics` to JWT only (scraper cannot authenticate) | S29 | Set `info.env` false; restrict management port or IP allowlist (infra, see D9) |
| H4 | Swagger public in prod | S30 | Disable in prod (`springdoc.swagger-ui.enabled: false`, `springdoc.api-docs.enabled: false`) |
| H5 | `profiles.active: dev` default | S31 | Remove from base; add startup guard refusing dev profile when `APP_ENV=prod` |
| H6 | Secrets: already fail-fast (JWT, admin password) | S32, S33 | No change; keep the existing `JwtUtilTest`; add a missing-secret context test for prod |
| L1 | Plaintext email in logs | S11, S12, S14 | Mask (`a***@d***.com`); or log `userId` only |
| L2 | Plaintext phone in logs | S13 | Mask (`+260 97****123`) |
| L3 | Account identifiers and wallet accounts in logs | S15, S16 | Mask or drop |
| L4 | Free-text KYC rejection reason in logs | S16 | Drop from log; keep in audit (with DEC) |
| L5 | Full stack trace for unhandled errors may contain SQL parameters | S2 | Redaction filter on log encoder; keep stack trace server-side only |
| L6 | MDC requestId not propagated to async audit threads | S20 | `TaskDecorator` copying MDC; `AsyncUncaughtExceptionHandler` that logs at ERROR with requestId |
| L7 | No log-leak scan test (BE-1C-8 §2 test) | S19 | Add `LogLeakScanIT` |
| A1 | Audit writes are fire-and-forget. A failed audit write does not fail the action. Payout and aspect paths swallow errors | S21, S22 | Owner decision D7 (failure policy). Recommendation in §7 |
| A2 | `ipAddress` never populated; no request id column | S23 | Pass IP and request id through the audit call (or the MDC), add nullable `request_id` column (V17 if needed) |
| A3 | Admin and money actions not audited: KYC approve, KYC reject, fee CRUD, KYC document download | S25 | Add audit records with before and after values |
| A4 | Auth events not audited (login, logout, password change, PIN, reset, verify) | S25 | Security events go to WARN log with requestId (item 6). Audit rows only if D8 says so |
| A5 | Security events: revoked-token use is DEBUG; access denied and 401 are silent; token-reuse log not found | S26 | Promote to WARN with requestId; add log lines |
| A6 | Startup `dev` profile and seeding (SEC-04) | S31 | As H5 |

Audit coverage table (BE-1C-8 §5, "table of every admin and money action"):

| Action | Route / caller | Audited today | Evidence | Gap / change |
|---|---|---|---|---|
| Wallet freeze | `POST /admin/wallets/{id}/freeze` | Yes (`WALLET_FREEZE`) | `wallet/service/WalletService.java:83`; `admin/service/AdminService.java:103` | Add IP and request id |
| Wallet unfreeze | `POST /admin/wallets/{id}/unfreeze` | Yes (`WALLET_UNFREEZE`) | `WalletService.java:98`; `AdminService.java:91` | As above |
| User soft-delete / restore | `PATCH /admin/users/{id}/deactivate` | Yes (`USER_TOGGLE_DELETE` plus wallet freeze/unfreeze) | `AdminService.java:110` | As above |
| KYC approve (creates wallet) | `POST /admin/kyc/{userId}/approve` | **No** | `kyc/service/AdminKycService.java:105-147` (no audit call) | Add `KYC_APPROVE` with old/new tier and status; wallet creation as separate row |
| KYC reject | `POST /admin/kyc/{userId}/reject` | **No** | `AdminKycService.java:158-182` | Add `KYC_REJECT` |
| KYC document download (PII read) | `GET /admin/kyc/{userId}/documents/{id}` | **No** | `AdminKycController.java:94-99`; `AdminKycService.java:92` | Decision D8 (recommend audit) |
| Fee config create | `POST /admin/fees` | **No** | `fee/controller/FeeController.java:135-146`; `fee/service/FeeEngineService.java:124` | Add `FEE_CREATE` |
| Fee config update | `PUT /admin/fees/{id}` | **No** | `FeeController.java:121-131`; `FeeEngineService.java:109` | Add `FEE_UPDATE` with before/after |
| Fee config delete | `DELETE /admin/fees/{id}` | **No** | `FeeController.java:148-155`; `FeeEngineService.java:143` | Add `FEE_DELETE` |
| Admin transaction and audit-log search | `GET /admin/transactions`, `/admin/audit-logs` | No | `AdminWalletController.java:157`, `:200` | Decision D8 (low priority) |
| Withdraw | `POST /wallets/{id}/withdraw` | Yes (`WITHDRAWAL`) | `TransactionService.java:175` | Verify old and new balance are recorded |
| Transfer | `POST /transfers` | Yes (`TRANSFER_DEBIT`, `TRANSFER_CREDIT`) | `TransactionService.java:339`, `:342` | Same as withdraw |
| Deposit | `POST /wallets/{id}/deposit` | N/A (always 503, no balance change) | `TransactionService.java:100-108` | Optional: audit rejected attempt (D8) |
| Payout initiate / success / compensated / pending | `POST /wallets/{id}/payouts`, webhooks, reconciliation | Yes | `PayoutService.java:354`, `:483`, `:519`, `:534`; `PayoutReconciliationService.java:258` | Best-effort (see A1) |
| Webhook quarantine | `POST /webhooks/lipila/disbursements` | Yes | `WebhookService.java:391`, `:394` | None |
| Collections callback (contained) | `POST /webhooks/lipila/collections` | No row (logs only, `WebhookService.java:161`, `:171`) | — | Decide: audit a contained callback (D8) |
| Auth: login, logout, password change, PIN set/change/verify, reset, verify | auth and user controllers | No (logs only) | `AuthService.java:99-259`; `UserService.java:76` | Security-event WARN with requestId (item 6), audit rows only per D8 |

## 3. Files to change

Error envelope and request id:
- `exception/ErrorResponse.java`: add `requestId`, change `errors` to `fieldErrors` (array of `{field, message}`). API change (§4).
- New `exception/ErrorCodes.java`: constants for every code in `docs/api/ERROR_CODES.md`.
- New `exception/ErrorBodyWriter.java`: single writer used by `GlobalExceptionHandler`, `RateLimitFilter`, `JwtAuthFilter`, `RestAuthenticationEntryPoint`.
- `exception/GlobalExceptionHandler.java`: map 404 and 405 correctly (E5), 409 for constraint violations (E8), 400 for `IllegalArgumentException` from tier lookup (E7), no raw input echo (S4), keep stack trace server-side only.
- `filter/RequestTracingFilter.java`: validate `X-Request-ID` against `[A-Za-z0-9-]{1,64}`; generate otherwise; `@Order(Ordered.HIGHEST_PRECEDENCE)` and register ahead of security (confirm ordering by test).
- `security/RateLimitFilter.java`, `security/JwtAuthFilter.java`, `security/RestAuthenticationEntryPoint.java`: use `ErrorBodyWriter`.

Headers, CORS, actuator, Swagger, profile:
- `config/SecurityConfig.java`: `Referrer-Policy`; explicit CORS header list; prod origins required.
- `src/main/resources/application.yml`: remove `profiles.active: dev` (H5); `management.info.env.enabled: false` (H3).
- `src/main/resources/application-prod.yml`: Swagger off (H4); no localhost CORS defaults (H2); management restricted (H3, infra).
- New startup guard (`config/ProductionGuard.java`): refuse dev profile when `APP_ENV=prod`; fail when prod CORS origins are blank or contain localhost.

Logging and redaction:
- New `security/LogMasker.java`: static `maskEmail`, `maskPhone`, `maskAccount`, plus reuse `SensitiveDataRedactor`.
- `auth/service/AuthService.java` (lines 99, 118, 124, 217, 237, 259): replace email with masked email or `userId`.
- `security/LoginRateLimiter.java` (48, 52, 66): hashed key, no email.
- `notification/service/SmsService.java` (57, 68, 71): masked phone.
- `notification/MailService.java:124`: masked recipient.
- `payment/LipilaGateway.java` (95, 102): masked account.
- `kyc/service/AdminKycService.java` (147, 182): drop wallet account and reason from logs.
- `bootstrap/AdminDataInitializer.java:86`: dev only; keep but mask.
- `security/JwtAuthFilter.java:110`: promote to WARN, keep `jti` only.
- `src/main/resources/logback-spring.xml`: redaction filter on JSON and file appenders (defence in depth).
- `config/AsyncConfig.java`: `AsyncConfigurer` with MDC-copying `TaskDecorator` and `AsyncUncaughtExceptionHandler`.
- `GlobalExceptionHandler.java:163`: stack trace stays server-side; redaction on message.

Audit:
- `audit/service/AuditService.java`: overload that takes IP and request id; failure policy per D7.
- `audit/aspect/AuditedAspect.java:176-179`: follow D7.
- `payment/service/PayoutService.java:715-722`: follow D7.
- `kyc/service/AdminKycService.java` (approve, reject, document read): add audit calls (D8).
- `fee/service/FeeEngineService.java` (create, update, delete): add audit calls with before and after.
- Optional V17 `audit_logs.request_id VARCHAR(64) NULL` (expand only), if D8 requires it.

API docs:
- Error catalogue: BE-1C-8 names `docs/api/ERROR_CODES.md`. The existing `docs/api/` folder is at the repo root (`NovaWallet/docs/api/`, holding `CONTRACT_TARGET.md` and `README.md`), not under `novawallet-api/`. Path to be confirmed by the owner (D10).
- `reports/API_ENDPOINT_INVENTORY.md`: update for changed responses.

## 4. Data / migration changes

- Only if D7 or D8 requires request id on audit rows: `V17__add_audit_request_id.sql` (or the next free version after Batch 7's V17 if D2 = DB): `ALTER TABLE audit_logs ADD COLUMN request_id VARCHAR(64) NULL;`. Expand only. No backfill. Rollback: drop column.
- No change to `audit_logs` immutability (DEC-06). Verify its status before editing: not verified in this pass.
- Verification: fresh V1→head and upgrade from previous head on PostgreSQL 16, `flyway validate`.

## 5. API changes

Common error envelope (all failure paths, including filters, 404/405, multipart, DB, provider):

```
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "requestId": "3f2a9c1e-...",
  "timestamp": "2026-11-14T10:00:00",
  "path": "/api/v1/...",
  "fieldErrors": [ { "field": "amount", "message": "must be positive" } ]
}
```

Changes against today:
- `requestId` added to every body; `X-Request-ID` header on every response, including 401/403 (depends on E-ordering test).
- `errors` map becomes `fieldErrors[]`. Breaking for any client reading `errors`. The app (`novawallet-app/`) and admin console must be checked. Those are out of scope for this prompt: report only.
- 404 unknown route → `404 RESOURCE_NOT_FOUND` (today likely 500, E5). 405 → `405 METHOD_NOT_ALLOWED`.
- Constraint violation → `409 CONFLICT`.
- Upload too large → 413 `PAYLOAD_TOO_LARGE` (unchanged).
- Unknown KYC tier → 400 `BAD_REQUEST`, generic message.
- Type mismatch → 400 generic message, no echo.
- Codes in use today (from `GlobalExceptionHandler`): `RESOURCE_NOT_FOUND`, `DUPLICATE_RESOURCE`, `BAD_REQUEST`, `UNAUTHORIZED`, `RATE_LIMITED`, `FORBIDDEN`, `SERVICE_UNAVAILABLE`, `VALIDATION_ERROR`, `PAYLOAD_TOO_LARGE`, `INTERNAL_SERVER_ERROR`.

## 6. Risks and how each is tested

| Risk | Test |
|---|---|
| Error envelope differs by failure class | `ErrorEnvelopeContractIT`: one case per class (400 validation, 400 malformed JSON, 401 no token, 401 revoked, 403 access denied, 404 unknown route, 405 wrong method, 409 constraint, 413 upload, 429, 503 revocation store down, 500 generic). Asserts keys, `requestId` present and equal to header, no stack, no SQL, no class names, no internal host |
| Request id injection | `RequestIdFilterTest`: `foo\n[ERROR] x` → replaced; valid value kept; response header and body match |
| Request id missing on 401/403 | `RequestIdOrderingIT`: anonymous request to protected route has header |
| Security headers | `SecurityHeadersIT`: HSTS (on TLS), `nosniff`, `Referrer-Policy`, CSP for API |
| CORS | `CorsPolicyTest`: no `*` origin with credentials; prod with unset origins fails startup; localhost absent in prod |
| Actuator | `ActuatorExposureIT`: `/actuator/env` not 200 anonymously; `/actuator/info` has no env block |
| Swagger in prod | `SwaggerProdIT` (prod profile): 404 |
| Profile default | `ProfileDefaultTest`: no `dev` activation when `APP_ENV=prod`; base YAML has no `active:` key (CI grep) |
| Weak or missing secret | existing `JwtUtilTest` plus `ProdStartupFailsWithoutSecretIT` |
| Log leak scan | `LogLeakScanIT`: captures logs for register, login (success and failure), forgot and reset, PIN set and wrong PIN, SMS send, Lipila initiation. Asserts no token, PIN, password, full phone, full email, Authorization header. Expected to fail until L1–L3 are done (write first) |
| Stack trace and SQL not in body | `ErrorEnvelopeContractIT` (500 and 409 cases) |
| Audit: one row per admin and money action | `AdminActionAuditIT` (KYC approve/reject, fee CRUD, freeze, deactivate), `MoneyActionAuditIT` (withdraw, transfer, payout states) with old and new values |
| Audit failure policy | `AuditFailurePolicyIT`: force `AuditLogRepository.save` to throw; asserts the policy chosen in D7 (action rolls back, or action succeeds and the failure is logged at ERROR with requestId) |
| Async MDC | `AsyncMdcIT`: request id present in audit-thread log line |
| Security events WARN | `SecurityEventLogIT`: failed login, lockout, revoked token, access denied, 401 each log at WARN with requestId and no secrets |
| PII read audit | `KycDocumentReadAuditIT` (if D8 = audit) |

Negative tests required by `novawallet-api/AGENTS.md`: authz on fee and KYC admin paths (non-admin → 403, no row written); tampered `X-Request-ID`; oversize multipart.

## 7. Decisions needed

| ID | Question | Options | Recommendation | Status |
|---|---|---|---|---|
| D7 (proposed) | Audit failure policy | (a) best-effort, log at ERROR (current) · (b) money and admin actions fail when audit write fails (synchronous, same transaction) · (c) outbox table | (b) for money and admin actions; keep (a) for security-event logs. Needs `AuditService` move off `REQUIRES_NEW`/`@Async` for those paths | Needs owner. Blocks A1 |
| D8 (proposed) | Which actions get audit rows | KYC approve/reject, fee CRUD (yes); KYC document reads, admin searches, auth events (?) | Yes for KYC approve/reject, fee CRUD, document reads. Auth events stay as WARN logs | Needs owner. Blocks A3, A4 |
| D9 (proposed) | Actuator and Prometheus exposure | JWT only (current) · separate management port · IP allowlist | Separate management port or IP allowlist, set in infra | BLOCKED on infra (no infra config in repo) |
| D10 (proposed) | Where the error catalogue lives | `NovaWallet/docs/api/ERROR_CODES.md` (root, read by app) · `novawallet-api/docs/api/` | root `docs/api/` (shared with app and admin) | Needs owner |
| D11 (proposed) | Log masking format | full redaction · partial mask (`a***@d***.com`, `+260 97****123`) · drop | partial mask for support diagnostics; never tokens or PIN | Needs owner. AGENTS rule 9 supports masking |
| D12 (proposed) | `Referrer-Policy` value | `strict-origin-when-cross-origin` · `no-referrer` | `strict-origin-when-cross-origin` (confirm) | Needs owner |
| DEC-06 | Audit immutability (REVOKE UPDATE/DELETE) | — | Already decided; implementation status not verified in this pass | Verify before BE-1C-8 starts |
| SEC-04 | Remove `dev` default | — | Remove (register recommendation) | Ready |
| DEC-11 | Postgres test infra | `pgtest` · Testcontainers | `pgtest` | OPEN (needed for audit and V17 tests) |

## 8. Steps in order (each verifiable)

1. Owner decides D7, D8, D10, D11, D12. D9 is handed to infra (BLOCKED until then).
2. Write failing tests first: `LogLeakScanIT`, `ErrorEnvelopeContractIT` (fails on `requestId` and `fieldErrors`), `RequestIdFilterTest`, `CorsPolicyTest`.
3. `RequestTracingFilter`: validation and ordering. Test: `RequestIdFilterTest`, `RequestIdOrderingIT`.
4. `ErrorBodyWriter` and `ErrorResponse` change; `GlobalExceptionHandler` mapping (404, 405, 409, tier 400, no echo). Tests: `ErrorEnvelopeContractIT` green.
5. Migrate the four body writers to `ErrorBodyWriter`. Re-run Batch 7 limiter tests.
6. Headers, CORS, actuator, Swagger, profile default, `ProductionGuard`. Tests: `SecurityHeadersIT`, `CorsPolicyTest`, `ActuatorExposureIT`, `SwaggerProdIT`, `ProfileDefaultTest`, `ProdStartupFailsWithoutSecretIT`.
7. `LogMasker` and call-site changes (L1–L4). Tests: `LogLeakScanIT` green. `MailServiceRedactionTest` still green.
8. Async MDC and uncaught handler (L6). Test: `AsyncMdcIT`.
9. Audit coverage (A2, A3, A4 per D8). `AdminActionAuditIT`, `MoneyActionAuditIT`.
10. Audit failure policy (A1 per D7). `AuditFailurePolicyIT`.
11. Security event WARN lines (A5). `SecurityEventLogIT`.
12. Error catalogue file (E6, per D10) and `reports/API_ENDPOINT_INVENTORY.md` updates.
13. Gates: `./mvnw -B clean test` (counts against baseline 316 plus new), PG16 check if V17, `git diff --check`, secret scan of diff, regression: Batch 1 and Batch 2 tests unchanged and green.

## 9. Out of scope / risks noted
- Reverse-proxy and actuator network exposure (D9): infra.
- Client code that reads `errors` (app, admin): the envelope change is breaking. The app and admin console are out of scope for this prompt; report only, and track as a follow-up prompt.
- DEC-06 status not verified; BE-1C-4 scope.
- KYC document reads (PII): retention is DEC-08 (BLOCKED on counsel).
- Inferred, to confirm by test: 404/405 currently become 500 (E5); request id absent on 401/403 (E-ordering); `ip_address` always null (A2 — verified by call sites, which use the 6-argument overload).
- Risk for BE-1C-2 follow-up: `GET /transactions/{reference}` 403 vs 404 leaks existence (from Batch 2 plan), relevant to the error envelope.
