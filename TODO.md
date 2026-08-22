# NovaWallet — Remediation Tracker

> **Last updated:** 2026-08-21 (orchestrator-maintained)
> **Source:** Multi-lens rating 2026-08-21 — Overall **7.2/10**, production band **Risky (68/100)**
> **Rule:** ❌ NO git commits without explicit user approval.
> **Verification:** Orchestrator runs the authoritative `./mvnw test` + `flutter analyze` + `flutter test` after each wave.

---

## ✅ Wave 1 — COMPLETE (4 parallel subagents)

### 🔴 B1 — Secrets cleanup (backend) · Agent A · ✅ DONE
- [x] Removed hardcoded JWT secret default from base `application.yml` → bare `${JWT_SECRET}`
- [x] Admin credential defaults relocated to dev-only profile with warnings
- [x] `AdminDataInitializer`: seeding gated behind dev profile; password never logged
- [x] All Java-side `@Value` secret fallbacks removed → prod fail-fast strengthened
- [x] 4 test files updated (`@ActiveProfiles({"dev","test"})`)
- Files: application.yml, application-dev.yml, AdminDataInitializer.java, 4 test files, prod yml comment
- ⚠️ Noted risks: git history retains old dev secrets (treat as burned); custom profiles without yml now fail fast (intended); mail creds don't fail-fast in prod (follow-up)

### 🔴 B2 — Per-user idempotency scoping (backend) · Agent B · ✅ DONE
- [x] Dead userId extraction block removed from `IdempotencyFilter`
- [x] Keys scoped per-user via composite `"<userId>:<key>"` storage (strategy (a) — zero schema risk; option (b) rejected because SQL UNIQUE treats NULLs as distinct, breaking anonymous-endpoint dedup)
- [x] Filter ordering verified: runs after JwtAuthFilter via SecurityConfig — no reorder needed
- [x] New tests: I8 cross-user same-key independence; scheduler double-run safety; replay asserts exactly 1 row
- [x] Bonus: >200-char keys now return clean 400 instead of confusing 409
- Files: IdempotencyFilter.java, IdempotencyService.java, 2 integration test files
- ℹ️ Thread.sleep polling left as-is (non-trivial fix, queued as follow-up)

### 🟡 F1 — Externalize LAN IP (frontend) · Agent C · ✅ DONE
- [x] Android default → `http://10.0.2.2:8080/api` (emulator host loopback), documented
- [x] Zero occurrences of `192.168.8.100` remain in repo
- [x] No tests referenced AppConfig defaults — no breakage
- File: app_config.dart
- ⚠️ Physical devices now need explicit `--dart-define=API_BASE_URL`

### 🟡 F2 — Dark mode + token sweep (frontend) · Agent D · ✅ DONE
- [x] `SemanticDarkColors` added mirroring semantic layer purpose-for-purpose (M3 tone inversion from reserved primitives only — no invented hexes)
- [x] `AppTheme.dark` built; shared `_buildTheme(ColorScheme)` extracted; light output byte-identical
- [x] Wired `darkTheme:` + `themeMode: ThemeMode.system`
- [x] dashboard_screen.dart fully swept: zero raw hex/NovaColors remain
- [x] New dark-theme contract test: ≥4.5:1 text contrast across all surface tiers + light regression guard
- Files: design_tokens.dart, app_theme.dart, app.dart, dashboard_screen.dart, new test
- ⚠️ Known follow-ups: core widgets (PillButton outlined/text variants, skeleton) still read static light tokens; cards_screen.dart has raw hexes

---

## ✅ Verification Pass (orchestrator) — ALL GREEN

| Check | Result |
|---|---|
| `./mvnw test` | ✅ **130/130 passed** — BUILD SUCCESS (incl. 3 velocity-limit tests) |
| `flutter analyze` | ✅ No issues found |
| `flutter test` | ✅ **214/214 passed** (incl. biometric service/provider/screen/router suites) |

### Pre-existing failures found & fixed during verification (NOT caused by Wave 1)
1. **`login_onboarding_screen_test.dart`** — asserted copy that doesn't exist ("Log in to your wallet", Checkbox, "Continue with phone", "Protected by 256-bit encryption"). Screen had evolved to Google sign-in; test was stale. Updated to match actual UI.
2. **Email-gate fallout from commit `3865a34`** — `requireVerifiedEmail` was added to withdraw/transfer without updating integration tests → 403s. Fixed by adding `verifyEmailFor()` helper (EndpointTestSupport) + `markEmailVerified()` in TransactionFlowIntegrationTest setup.

---

## ✅ Wave 2 — COMPLETE

### 🟡 Q1 — Transaction velocity limits (backend) · Agent · ✅ DONE
- [x] `enforceVelocityLimits(userId, amount)`: 60-min rolling window, count cap (default 10/h) + amount cap (50,000 ZMW/h)
- [x] Configurable via `app.transaction.velocity` (@ConfigurationProperties); no migration needed
- [x] Enforced before PIN verification; typed 429 RATE_LIMITED errors
- [x] 3 integration tests (count cap / amount-cap independence / window expiry)

### 🟡 Q2 — Biometric unlock (frontend) · Agent · ✅ DONE
- [x] local_auth ^2.3.0 wrapper; Hive-backed flag default OFF; capability-gated settings toggle
- [x] Lock screen on cold start + background resume; retry UI + password fallback; PIN-set flow outranks lock (router bug found & fixed: gate was hijacking /pin/set redirects)
- [x] Android USE_BIOMETRIC + FlutterFragmentActivity; iOS NSFaceIDUsageDescription
- [x] Compile fixes applied by orchestrator (local_auth getter/method signatures, AuthState reads, mocktail fallback registration, double-dispose in tests)
- ⚠️ Needs manual on-device testing (enrolled biometrics, permission denial, mid-prompt backgrounding)

## ✅ Documentation Refresh — COMPLETE
- [x] Root `README.md` rewritten (117 lines): architecture, quickstart, testing, stack
- [x] `docs/PROJECT-PLAN.md` recreated (426 lines): Phases 0–9 ✅, Post-MVP Hardening ✅, Phase 10 substantially ✅ (FCM 10.7 ⏳), Phase 11 ⏳ + Zambia rails recommendation (Flutterwave primary; Paystack does not cover Zambia; virtual cards deferred — Sudo/Kora/Fyatu options noted)

---

## 📋 Queued (Wave 2+ — sequenced to avoid conflicts)

| # | Task | Blocked by / reason |
|---|---|---|
| Q1 | Transaction velocity limits (backend) | After B2 ✓ — clear now; owns next Flyway migration |
| Q2 | Biometric unlock via `local_auth` (frontend) | After F2 ✓ — clear now |
| Q3 | Postgres-backed money-path integration run | Needs Docker running; kills H2/PG parity risk |
| Q4 | Split 5 oversized screens (>400 LOC): send 710, dashboard 695, widgets 647, cards 594, fee_config 591 | Clear |
| Q5 | Stricter lint rules (`strict-casts`, etc.) | After Q4 |
| Q6 | Dark-mode sweep of core widgets + cards_screen raw hexes | Follow-up from F2 |
| Q7 | Replace IdempotencyFilter Thread.sleep polling | Follow-up from B2 |
| Q8 | Prod mail creds fail-fast | Follow-up from B1 |

## 📝 Progress Log

- **2026-08-21** — Tracker created. Wave 1 dispatched: Agents A/B (backend), C/D (frontend).
- **2026-08-21** — Wave 1 complete: all 4 agents reported success. Verification: backend initially 124/127 (3 pre-existing 403s from untested email gate commit `3865a34`) → fixed test fixtures → **127/127 green**. Frontend: analyze clean; 181/182 → fixed stale login test assertions → **182/182 green**.
- **2026-08-21** — Wave 1 committed across 8 conventional commits (gitignore restore, docs consolidation, api secrets hardening, api idempotency scoping, ui dark mode, app polish wave, config fix, test fix) and pushed to origin/main.
- **2026-08-22** — Session paused mid-Wave-3. IN TREE, UNVERIFIED+UNCOMMITTED: Agent A payments scaffold (Lipila gateway/webhooks/payouts, 14 tests) + Agent B dark sweep (pill_button/skeleton/cards_screen + demo-data extraction). NOT DONE: screen splits (agent failed 2×), idempotency backoff + prod mail fail-fast. Full state, prompts, research digest: **HANDOFF_2026-08-22.md**.
- **2026-08-21** — Wave 2 complete: velocity limits (backend) + biometric unlock (frontend). Orchestrator fixed 9 compile/test defects from the cancelled first biometric attempt. Verified: backend 130/130, frontend analyze clean + 214/214. Docs refreshed (README.md + docs/PROJECT-PLAN.md). Branches: master fully contained in main → deleted on remote + local; single-branch repo.
