# S0-01 — Verify Phase 1C Batch 1 and prepare the owner commit

Date: 2026-11-13 · Branch: `feature/phase-1c-security-database-hardening` · Repo: `novawallet-api/`

## File classification (`git status --short`)
**Source (modified, 20):** `admin/service/AdminService`, `auth/controller/AuthController`, `auth/service/AuthService`, `config/SecurityConfig`, `notification/MailService`, `notification/dto/NotificationResponse`, `payment/dto/PayoutRequest`, `payment/service/PayoutService`, `security/JwtAuthFilter`, `security/JwtUtil`, `token/service/TokenService`, `transaction/dto/{Deposit,Transfer,Withdraw}Request`, `transaction/service/TransactionService`, `user/service/UserService`, `application.yml`, `application-dev.yml`, `application-prod.yml`, `.env.example`, `docker-compose.yml`
**Source (new, 8):** `security/{EmailVerificationPolicy,JwtClaims,RestAuthenticationEntryPoint,SensitiveDataRedactor}`, `token/entity/RevokedAccessToken`, `token/repository/RevokedAccessTokenRepository`, `token/schedule/RevokedAccessTokenCleanupJob`, `token/service/AccessTokenRevocationService`
**Migration (new):** `V16__create_revoked_access_tokens.sql`
**Tests (modified 11, new 9 + dirs `notification/dto/`, `token/`):** `LogoutFailureAtomicityIntegrationTest`, `LogoutIntegrationTest`, `SessionRevocationIntegrationTest`, `MailServiceRedactionTest`, `EmailVerificationPolicyTest`, `JwtAuthFilterFailClosedTest`, `JwtUtilTest`, `SensitiveDataRedactorTest`, plus `application-test.yml`
**Docs:** `docs/architecture/backend-risk-register.md` (M), `docs/phase-1c/` (new), `AGENTS.md` (new)
**Unexpected — owner decides:**
- 5 **deleted** files `report/PHASE_1A_*.md` (likely moved during the 13 Nov cleanup). Either `git rm` them in this commit or `git restore report/` first.
- No `target/`, `uploads/`, `.env`, logs or IDE files untracked.

## Evidence
| Check | Command | Result |
|---|---|---|
| Tests | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test` | **Tests run: 316, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**, exit 0 |
| PG16 fresh | flyway/flyway:10 vs postgres:16 container, `migrate` + `validate` | 15 migrations applied, at v16; validate OK |
| PG16 upgrade | `migrate -target=15` then `migrate` + `validate` | v15 → v16 applied (1 migration); validate OK |
| Whitespace | `git diff --check` | exit 0 |
| `.gitignore` | grep | covers `target/`, `uploads/`, `.env`/`.env.*` (keeps `.env.example`), `*.log` — no change needed |

Notes:
- Migration **V12 does not exist** (V11 → V13). Pre-existing in committed history; Flyway is fine with it. Do not renumber.
- Environment: machine had JREs only (no `ct.sym`), so compile failed with `release version 17 not supported`. Installed `openjdk-21-jdk-headless`. **Use `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`** (default `java` is 25 JRE).

## Secret scan
- No real secrets in the diff or untracked files.
- Test fixtures only: `application-test.yml` JWT secret decodes to "test-only-jwt-secret…"; a base64 Lipila webhook secret in `DisbursementWebhookControllerTest` / `DisbursementWebhookServiceTest` (already committed, test-only — owner confirm it is not a real sandbox secret).
- Test phone numbers (`+260971234567`, `260961234567`, `+260955000001`) look synthetic — owner confirm.

## Owner commit commands
```
cd novawallet-api
git restore report/                          # recommended: Phase 1A reports exist nowhere else (root reports/ has only 1B)
git add AGENTS.md .env.example docker-compose.yml docs/ src/
git status --short                           # review: nothing unexpected staged
git commit -m "Phase 1C Batch 1: auth & session hardening (SEC-01,02,03,06,08, NEW-AUTH-01)"
```

## Report — S0-01
Done: verified Batch 1; 316/316 tests; PG16 fresh + upgrade + validate; diff check; secret scan; commit plan.
Files changed: 2 (`reports/opencode/README.md`, this report). No code changed.
Evidence: above table.
Blocked / not done: none. Commit is owner-only.
Decisions needed: keep or delete `report/PHASE_1A_*` (recommend **keep** via `git restore report/` — no other copy of the Phase 1A reports exists).
Risks noticed (not fixed): default JDK is a 25 JRE — set JAVA_HOME or `update-alternatives` (→ OPS-01); V12 gap (cosmetic).
Next: S0-02 (`prompts/stage-00_housekeeping/S0-02_workspace_check.md`) — after owner commits.
