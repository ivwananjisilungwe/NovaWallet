# NovaWallet — Remaining Work: Phase Plan & Delegated Todos

**Date:** 2026-08-21
**Derived from:** `reports/UI-UX-ASSESSMENT.md` (final verdict), `reports/UI-MODERNIZATION-REPORT.md`, `.planning/codebase/CONCERNS.md`, `NOVAWALLET_FINTECH_POLISH_PLAN.md`.
**Execution model:** each task is a single delegated agent call (category + skills). Orchestrator verifies, never implements. Run phases in order; tasks inside a phase are parallelizable unless marked serial.

---

## Phase 1 — UI Token Consistency (highest ROI, small blast radius)

**Goal:** eliminate every hardcoded color bypassing the token system; one canonical BalanceCard.
**Exit criteria:** `grep -rn "Color(0xFF" novawallet-app/lib/features` returns zero non-token hits outside theme files; dashboard renders identical layout with shared components; `flutter test` green.

| # | Delegated todo | Category | Skills |
|---|---|---|---|
| 1.1 | Delete private `_BalanceCard` + `_SlidingGradientTransform` from `dashboard_screen.dart`; compose the shared token-faithful `BalanceCard` (`core/widgets/widgets.dart`) preserving shimmer + masked account behavior | `visual-engineering` | frontend |
| 1.2 | Re-tokenize Quick Actions in `dashboard_screen.dart` (`0xFFF59E0B`→`NovaColors.warning`, `0xFF10B981`→emerald/success token) and avatar gradient → `NovaColors.primary→primaryContainer` | `visual-engineering` | frontend |
| 1.3 | Repo-wide sweep for remaining inline hex colors across features/ (auth, wallet, kyc, extras) → replace with semantic tokens; keep theme files untouched | `unspecified-high` | frontend |

## Phase 2 — Pixel-Fidelity Verification (closes the "unverified" gap)

**Goal:** deterministic proof of fidelity vs Stitch references.
**Exit criteria:** golden/screenshot diff report per key screen committed to `reports/pixel-fidelity/`; deltas >2% listed with cause.

| # | Delegated todo | Category | Skills |
|---|---|---|---|
| 2.1 | Golden-test harness: render welcome/login/dashboard/cards/deposit at Stitch viewport sizes (mobile), compare against `stitch_duplicate_of_novawallet/*/screen.png`; produce per-screen delta table | `deep` | e2e-testing, playwright |
| 2.2 | Triage any >2% deviations → classify as fixable drift vs intentional enhancement; write `reports/pixel-fidelity/TRIAGE.md` | `unspecified-low` | — |

## Phase 3 — Dark Mode (tokens already reserved)

**Goal:** activate the reserved dark palette (`design_tokens.dart:67-78`) as a real `darkTheme`.
**Exit criteria:** dark theme passes same widget tests; manual QA checklist for balance card, nav bar, tiles.

| # | Delegated todo | Category | Skills |
|---|---|---|---|
| 3.1 | Uncomment/extend dark primitives → build `SemanticColors.dark*` aliases → wire `darkTheme:` in `app_theme.dart` with Material 3 surfaces | `visual-engineering` | frontend |
| 3.2 | Add dark-mode widget tests mirroring `widget_test.dart` palette assertions | `quick` | — |

## Phase 4 — Backend Hardening (release blockers from CONCERNS.md)

**Goal:** close money-safety and security gaps in `novawallet-api` (Spring Boot).
**Exit criteria:** idempotency persisted+scoped per user; unverified emails cannot transact; change-password endpoint live behind auth; dev secrets out of git; 125+ tests green.

| # | Delegated todo | Category | Skills |
|---|---|---|---|
| 4.1 | Persist idempotency keys (DB table keyed by user_id + key, unique constraint, replay returns original result) — fixes non-persistent in-memory keys flagged as **money bug** | `backend` | postgres-patterns, database-migrations |
| 4.2 | Gate financial endpoints on verified email (403 pre-verification); align with existing `verify_email_screen.dart` flow | `backend` | springboot-security |
| 4.3 | Add authenticated change-password endpoint (current-password check, rehash, revoke sessions); match Flutter `change_password_screen.dart` contract | `backend` | springboot-security |
| 4.4 | Remove committed dev DB password from repo history surface; move to env/profile config; audit for other secrets | `backend` | security-bounty-hunter |
| 4.5 | Balance-cache consistency: define invalidation on every mutation path (deposit/withdraw/send); add concurrency test | `ultrabrain` | redis-patterns |

## Phase 5 — Ship Gate (serial)

| # | Delegated todo | Category | Skills |
|---|---|---|---|
| 5.1 | Full verification pass: `flutter analyze` (zero new issues), `flutter test`, `./mvnw test`, CodeScene change-set review vs main | `unspecified-high` | codehealth-mcp |
| 5.2 | Update handoff docs (`HANDOFF_2026-08-21.md` successor entry) with phase outcomes | `writing` | — |

---

## Orchestration notes
- Phases 1.x can fan out as 3 parallel agents; 4.1–4.4 as 4 parallel agents.
- Subagent dispatch currently blocked by OpenCode billing error (`No payment method`) — resolve at opencode.ai/workspace billing before delegating, or execute tasks directly if urgent.
- Do NOT restart completed reports; append phase outcomes only.
- Preserve GSD session state in `.planning/`.
