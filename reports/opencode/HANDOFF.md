# Session handoff → opencode

## UPDATE 2026-11-14 (Claude Code session) — read this first
- **BE-1C-2 is built and tested.** Steps 1–5 of "remaining steps" below are DONE: the full suite was 361 run / 0 failures / 0 errors / 0 skipped. `RouteSecurityEnumerationTest`, `InputBoundsIntegrationTest` and `AuthorizationGapIntegrationTest` (5/0) were added, `reports/PHASE_1C_AUTHZ_MAP.md` was written (48 routes; its 6 gaps are closed by the gap test), and SEC-13 (single ADMIN, DEC-03) is in the risk register.
- Batch report / traceability / `BE-1C-2_REPORT.md`: see `PROGRESS.md` for whether they landed.
- **Git:** branch `feature/be-1c-2-authz` is pushed to origin. Owner rule: commit + push each finished, tested step to a feature branch, never to main.
- **Next:** T1 independent review of BE-1C-2 (new session), then BE-1C-3. That needs owner answers to DEC-04 and DEC-11 and approval of `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_3_PLAN.md`.
- Plan-only docs (awaiting approval): `PHASE_1C_BATCH_{3,4,6,7,8}_PLAN.md` in `novawallet-api/docs/phase-1c/`.
- Uncommitted files NOT from that session (opencode's audit work): README.md, StartHere.md, TODO.md, HANDOFF.md, docs/PROJECT_ROADMAP.md, docs/PROJECT_STATE_AUDIT.md. Commit them yourself if you want them kept.

---

Written 2026-11-13 by the previous agent (Claude Code), stopped mid-prompt at the user's request.
Start here, then read `PROGRESS.md` (same folder), root `AGENTS.md`, `novawallet-api/AGENTS.md`, and `docs/DECISIONS.md`.

## Where things stand
- **Prompts done:** S0-01, S0-02, S0-03 (reports in this folder).
- **In progress:** **BE-1C-2** (`prompts/stage-01_backend-security/BE-1C-2_authorization_ownership.md`, with `_COMMON_BATCH_RULES.md`). Implementation is mostly done. **Tests have NOT been run since these changes.** Only `./mvnw -q compile` passed.
- **Decisions recorded today** in `docs/DECISIONS.md`: DEC-03 = single ADMIN now, roles in BE-1D-07; DEC-05 = idempotency-key scope; DEC-06 = DB revoke UPDATE/DELETE; DEC-12 = keep the fraud freeze on restore; DEC-24 = monorepo (A).
- **Git:** nothing committed (agents never commit). S0-01's Batch 1 work is still uncommitted in `novawallet-api`, so Batch 2 changes now sit on top of it in the same working tree.

## Build environment (important)
- Use `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. The default `java` is a 25 JRE and fails with "release version 17 not supported".
- Test command: `cd novawallet-api && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test` (~5 min; baseline **316 tests, 0 failures**).
- PG16 migration check (only if a migration is added; Batch 2 adds none): disposable `postgres:16` + `flyway/flyway:10` containers on a docker network; fresh V1→head, upgrade prev→head, `validate`. Docker works without sudo. V12 does not exist (historical; never create it).
- `actionlint` is not installed.

## BE-1C-2: done so far (uncommitted, untested)
Plan: `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_PLAN.md` (findings F1–F9; read it first).
| Finding | Change made | Files |
|---|---|---|
| F1 ownership already enforced in services | no code change; tests added | — |
| F2 unbounded paging | `PageRequests.of(page,size[,sort])`: page ≥ 0, 1 ≤ size ≤ 100, else 400 | new `config/PageRequests.java`; `TransactionController`, `NotificationController`, `AdminUserController`, `AdminWalletController` (3 sites) |
| F3 no URL-level admin rule | `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` before `anyRequest()` | `config/SecurityConfig.java` |
| F4 multipart 1 MB default < KYC 10 MB | `spring.servlet.multipart.max-file-size: 10MB`, `max-request-size: 11MB`; `MaxUploadSizeExceededException` → 413 `PAYLOAD_TOO_LARGE` | `application.yml`, `exception/GlobalExceptionHandler.java` |
| F5 unknown JSON fields ignored | `spring.jackson.deserialization.fail-on-unknown-properties: true`; `@JsonIgnoreProperties(ignoreUnknown = true)` added to `GatewayStatusResponse`, `GatewayInitiationResponse` (`GatewayCallbackPayload` already had it) | `application.yml`, `payment/dto/*` |
| F6 DTO bounds | PIN `@Pattern ^\d{4,6}$` on Transfer/Withdraw/Payout; payout `accountNumber` `@Pattern ^\+?\d{9,15}$`; email `@Size(max=254)` on Login/Forgot/Register; login password max 128; reset token max 256; change-password current max 128; KYC reject reason max 500; CreateFee: type max 20, percentage ≤ 1.0000 with `@Digits(1,4)`, flat/min/max `@Digits(10,2)` ≥ 0 (matches V7 column types) | DTOs under `transaction/dto`, `payment/dto`, `auth/dto/request`, `user/dto/request`, `kyc/dto`, `admin/dto` |
| F7 fee estimate amount unbounded | scale ≤ 2 and ≤ 13 integer digits, else 400 | `fee/controller/FeeController.java` |
| F8 invalid KYC tier → 500 | `IllegalArgumentException` → `BadRequestException` (400) | `kyc/service/AdminKycService.java` |

Tests written (not yet run):
- New `src/test/.../endpoint/AuthorizationMatrixIntegrationTest.java`: wallet-id IDOR (read and money routes), reference IDOR, unknown/malformed ids, 401s, every admin route → 403 for a customer and 401 anonymous, and an admin positive case.
  - It builds a `Transaction` via `Transaction.builder()`. If it fails on non-null columns (e.g. `attemptCount`, `createdAt`), set them in the builder.
  - It assumes deposit/withdraw by a non-owner give 403 (ownership is checked before the disabled-deposit/PIN checks); verify this.
- Added test `payout_fromAnotherUsersWallet_isForbidden_andGatewayNotCalled` to `PayoutEnabledIntegrationTest.java`.

## BE-1C-2: remaining steps
1. **Run the full suite.** Fix any fallout. Likely fallout:
   - Existing tests that send extra JSON fields (F5).
   - PINs with non-digits, or payout account numbers not matching the new pattern (F6).
   - Tests that use `size` > 100 or `page` < 0 (F2).
   - Tests expecting 500 for a bad tier (F8).
   
   Never weaken a safeguard to make a test pass. If a test encodes the old unsafe behaviour, update it and say so in the report.
2. **Write `RouteSecurityEnumerationTest`.**
   - Autowire `RequestMappingHandlerMapping` and enumerate every pattern and method.
   - Replace path variables with a random UUID and send each request unauthenticated.
   - Expect 401 unless the route is in an explicit allow-list. The allow-list must match `SecurityConfig` permitAll: `/api/v1/auth/register`, `/auth/login`, `/auth/refresh`, `/password/forgot`, `/password/reset`, `/email/verify`, `/api/v1/webhooks/lipila/**`, swagger/api-docs, `/actuator/health`, `/actuator/info`.
   - Also assert that each allow-listed route still exists.
3. **Write `InputBoundsIntegrationTest`** with boundary values:
   - amount `0.001` and `10000000000000.00` → 400; `9999999999999.99` passes validation.
   - PIN `12a4` → 400.
   - MSISDN `abc` → 400.
   - `size=101`, `size=0`, `page=-1` → 400; `size=100` → 200.
   - Unknown field (`"role":"ADMIN"` or `"balance":1` on `PUT /users/me` and on transfer) → 400 (mass assignment, F9).
   - CreateFee percentage `1.5` → 400.
   - Fee estimate `amount=1.001` → 400.
   - KYC approve with tier 99 → 400.
   - Multipart > 10 MB → 413. Note that MockMvc may not enforce multipart size: if so, assert the config property and the handler mapping, and say so honestly.
4. **Generate `reports/PHASE_1C_AUTHZ_MAP.md`.** For every route list: method, path, required auth/role, ownership enforcement (file:line), and the proving test. Mark gaps. The data is already gathered in the plan and `docs/api/CONTRACT_TARGET.md`.
5. **Document the single-ADMIN risk** (DEC-03) in the canonical risk register `novawallet-api/docs/architecture/backend-risk-register.md`.
6. **Run the Batch Gate Protocol** (plan §13):
   - full suite counts, `git -C novawallet-api diff --check`, secret scan of the diff.
   - Update `PHASE_1C_RISK_TRACEABILITY_MATRIX.md` **only with executable evidence**.
   - Update root `TODO.md` with Batch 2 stages as checkboxes, like Batch 1.
   - Write `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_REPORT.md` (sections A–M like the Batch 1 report) and `reports/opencode/BE-1C-2_REPORT.md` (root AGENTS.md format).
7. Update `PROGRESS.md`, then **stop**. 🔒 BE-1C-2 needs an independent review next: `prompts/templates/T1_independent_review.md`, in a new session.

## Known risks to carry into reports (not fixed, out of scope)
- `TransactionHistoryService.getMonthlyStatement` totals only the first 100 transactions of a month → BE-1D-12.
- `GET /transactions/{reference}` returns 403 for another user's reference vs 404 for an unknown one, which reveals that a reference exists → contract change, raise at BE-99.
- `ChangePasswordRequest.newPassword` max is 100 while register/reset allow 128 (inconsistent; left as is).

## Owner actions still outstanding
1. Commit S0-01 (`S0-01_REPORT.md`; recommend `git restore report/` first).
2. Monorepo git-root move (`S0-02_REPORT.md`); move `prompts/NovaWallet/` (137 MB duplicate) and `Designs/` out first.
3. Fonts into `docs/v5/part-06/fonts/`.
4. Remaining open decisions in `docs/DECISIONS.md` (next ones needed: DEC-04, DEC-11 for BE-1C-3; DEC-09 for BE-1C-7).
5. The sudo password was shared in the previous chat: change it.
