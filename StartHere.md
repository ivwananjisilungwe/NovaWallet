# NovaWallet — START HERE

Student-first mobile wallet for Zambia. This folder is the **cleaned project** plus the **complete prompt pack** to build it to production with an AI coding agent (opencode, Codex, etc.). Cleaned on 13 Nov 2026.

## 1. What is in here
| Folder | What |
|---|---|
| `novawallet-api/` | Spring Boot 3.5.3 backend (your real code, **with its git history and your uncommitted Phase 1C Batch 1 work intact**). Rules: `novawallet-api/AGENTS.md` |
| `novawallet-app/` | Empty — the Flutter app is built here by prompts `APP-00…APP-23`. Rules: `AGENTS.md` inside |
| `novawallet-admin/` | Empty — the React admin console is built here by `ADM-00…ADM-08`. Rules inside |
| `infra/` | Empty — Docker, Caddy, CI/CD, monitoring, runbooks (`OPS-*`). Rules inside |
| `docs/v5/` | **The designs, unpacked**: `part-01…06, 09` app screens (A01…Q13: 127 screens, light + dark PNG/SVG + `BUILD.md`), `part-05` animations, `part-06` brand/tokens/icons, `part-07` admin console (X01…X16), `part-08` architecture/payments/cards/AI/compliance notes, `part-10` 26 system diagrams. **Index of every screen: `docs/v5/SCREEN_INDEX.md`** |
| `docs/` (other) | `SRS.md`, `AI-STRATEGY.md`, `CARD-ISSUING-RESEARCH.md`, `DIFFERENTIATOR-FEATURES.md`, `reference/COMPETITIVE-ANALYSIS.md` |
| `reports/` | Phase 1B final reports, endpoint inventory (`API_ENDPOINT_INVENTORY.md`), status report, test audit, PG16 verification |
| `TODO.md` | Current backend objective (Phase 1C) |
| `AGENTS.md` | Root rules for every agent (read first) |
| `prompts/` | **The build prompts, in order** — `prompts/01_MASTER_ORDER.md` |

What was removed and why: see `CLEANUP_REPORT.md`.

## 2. Where the project stands today (verified from your upload)
- **Backend:** Phase 1A and 1B done (safe payouts, reconciliation, webhooks). **Phase 1C Batch 1 done but uncommitted** (316 tests green per your reports). Batches 2–9 not started.
- **Flutter app:** does not exist yet. **Admin console:** does not exist yet.
- **Designs:** complete (127 app screens, 16 admin screens, animations, brand, 26 diagrams).
- **Gap to know about:** the designs expect features the API does not have yet (phone OTP sign-up, QR, requests/splits, bills, goals/Chilimba, insights, cards, support, admin roles…). `prompts/` builds them all, backend first.

## 3. The plan in one paragraph
Finish the **backend completely** (security → app API → double-entry ledger → payments → AI → architecture), freeze the API contract (`BE-99`), stand up staging, then build the **Flutter app screen by screen to match the designs pixel-for-pixel** (every screen is checked against the design PNG by an automated fidelity comparison), build the **admin console** the same way, harden, and launch with a closed beta. ~90 prompts, ordered in `prompts/01_MASTER_ORDER.md`.

## 4. First 30 minutes
1. **Read** `AGENTS.md` (root) and `prompts/01_MASTER_ORDER.md`.
2. **Commit your Phase 1C Batch 1 work** yourself (prompt `S0-01` verifies it and gives the exact commands). The agent will never commit.
3. **Put the fonts in place:** copy the Plus Jakarta Sans and Inter font files into `docs/v5/part-06/fonts/` (the folder was empty). Without them the pixel comparison cannot pass.
4. **Start the external items that take weeks** (list in `prompts/02_DECISIONS_AND_BLOCKERS.md` §C): Lipila collections signing spec + sandbox/live onboarding, a Zambian lawyer on the licence route and data-protection duties, bank/settlement account, Google Play organisation account, pilot partner at Mulungushi.
5. Run prompt **#1** (`prompts/stage-00_housekeeping/S0-01_commit_batch1_and_baseline.md`).

## 5. How to run each prompt
1. Open `prompts/01_MASTER_ORDER.md` → take the next unticked step (its **Needs** column must be ticked).
2. **New agent session.** Paste the prompt file content. Nothing else.
3. The agent works, runs the gates, writes a report, and **stops**.
4. 🔒 steps: open a **new** session, run `prompts/templates/T1_independent_review.md`. Fix findings with `T2_fix_findings.md`.
5. You run `prompts/templates/T4_owner_gate_checklist.md`, then **commit yourself**.
6. Tick the step. Next.
Big or fuzzy step? Prefix it with `T5_plan_first.md`. Session confused or cut off? `T6_resume_session.md`. Found a bug? `T3_bug_report.md`.

## 6. The rules that make it work
- **Backend before app.** The app is built only against the frozen contract from `BE-99`; no app screen is built on a guessed endpoint.
- **The designs are the truth.** The app/admin agents must reproduce each screen from its `BUILD.md` + SVG + PNG, then pass the automated fidelity compare (≤ 3 % pixel difference, both themes) before the screen counts as done. Every unavoidable deviation is logged for your approval (`docs/app/FIDELITY_LOG.md`).
- **You commit, the agent never does.** One prompt = one reviewable change set.
- **Decisions are yours.** Open decisions live in `docs/DECISIONS.md` (created by `S0-03`); the agent stops when it needs one.
- **BLOCKED means blocked.** No invented provider contracts, fees, limits or legal rules. Everything provider-dependent runs on a built-in sandbox simulator until the real contract exists.
- **Sandbox only** until `LAUNCH-02`. No live keys in any repo or prompt.
- **Nothing here is legal advice or a compliance claim.** Licensing, data protection, AML/CFT and terms need a Zambian lawyer and your licensed partner.

## 7. Honest expectations
- The agent will make mistakes; reviews, gates and the fidelity compare exist to catch them. Don't skip the 🔒 reviews.
- Prompts are tuned to your repo as of 13 Nov 2026. If the code differs from what a prompt assumes, the agent must report the difference, not guess.
- Your launch date is set by the outside items (provider contract, licence route, lawyer, pen-test), not by coding speed. Start them now.
- I (the author of this pack) could not compile or run your backend here, so test counts come from your own reports; your first run of `S0-01` is the real check.

## 8. Quick map of prompt families
`S0` housekeeping · `BE-1C-*` finish Phase 1C security · `BE-1D-*` APIs the app and admin need · `LED-*` double-entry ledger · `PAY-*` payment orchestration, Lipila, fees/limits, bills, cards · `AI-*` fraud, assistant, KYC assist · `ARC-*` modular monolith, outbox · `BE-99` backend complete gate · `OPS-*` containers, servers, CI/CD, backups, monitoring, secrets, security testing, load, runbooks · `APP-*` Flutter · `ADM-*` admin · `LAUNCH-*` compliance pack, go-live, beta, rollout, operations · `T1…T6` reusable templates.
