# Session Handoff — NovaWallet Stitch UI Design (coverage gaps)

**Date:** 2026-08-06
**Context:** NovaWallet (Spring Boot backend ✅ complete, ~150 Java files + 123 passing tests) + Stitch UI design (Phase 10 mobile app + admin console)
**Stitch project:** `projects/11542754846512650236` (title: **NovaWallet**, PRIVATE, PROJECT_DESIGN)
**Design system asset:** `assets/13836462354037157009` ("NovaWallet Design System" v1, LIGHT mode)

> See also the prior handoff: `.planning/2026-08-05-stitch-ui-design-handoff.md`.

---

## What this session did

Decided which missing screens a **professional Zambian fintech banking app** actually needs, and generated the justified ones in Stitch (skipping the V2/post-MVP "card creation" screen).

### Screens added (9 prompts in `.planning/prompts/26–34-*.txt`)

| No. | Slug | Screen purpose | Status | Stitch screen id(s) |
|-----|------|----------------|--------|----------------------|
| 26 | `26-notifications` | Notification inbox (Today/Earlier, unread + read states, mark-all-read) | ✅ done | `screens/b7f190f43c764c47a70ebe9682cdbb3b` |
| 27 | `27-transaction-filters` | Filter sheet: type / status / date range / amount | ⏳ retry | **FAILED** — `Request contains an invalid argument` (also timed out on retry; transient) |
| 28 | `28-statements` | Statements: month nav, totals, PDF/CSV download, preview | ✅ done | `screens/a22b6c7109014fa18fec4098ff492283` |
| 29 | `29-buy-airtime` | Buy airtime: +260 number, Airtel/MTN, amount grid, wallet pay | ✅ done | `screens/cd23f376be8c4b3fb06d12ba9cd83c7d` |
| 30 | `30-empty-state` | Empty transaction state + KYC tip card | ✅ done | `screens/1a65e87a7bbc47c182ac900f5f236edb` |
| 31 | `31-error-state` | Network/error modal (NWERR-504) + offline banner variant | ✅ done (2: modal + banner) | `screens/f4380527840a4a68b71e0b08fd470301` and `screens/6afa498df4dc4854860a4b921e56b486` |
| 32 | `32-loading-state` | Processing-transfer spinner + step tracker + ref id | ✅ done (2 variants) | `screens/fc95bf4b3f4c405bba0b0327b454185d` and `screens/d52957736ab841aeb2ce010077aa9f96` |
| 33 | `33-security-center` | PIN/password, devices, 2FA, recent activity, score meter | ✅ done | `screens/d5e5e0ce2e1b415e91d271ee613d9017` |
| 34 | `34-fee-disclosure` | Fee breakdown + daily limits + balance-after before transfer | ✅ done | `screens/cb16f32d38644e57b2573ec8e176092b` |

**8 of 9 generated successfully** (10 screen ids including the 2 error-state and 2 loading-state variants). The project now has **~40 screen instances** total.

### Why these and why skip card creation
- `31-error-state` and `32-loading-state` came back as **two variants each** — keep whichever fits best.
- `27-transaction-filters` failed twice (invalid-argument / timeout) — see "Retry" below.
- **Card creation form** (`20-virtual-cards` prompt exists but backend cards are V2/post-MVP) was **deliberately skipped** — do **not** generate until the card backend feature is implemented.

---

## Retry / next actions

1. **Retry the failed screen #27** (it currently has no screen):
   ```bash
   node .planning/stitch-gen.mjs 27-transaction-filters .planning/prompts/27-transaction-filters.txt
   # poll: cat /tmp/stitch_27-transaction-filters.json
   ```
   Note: the prompt is single-line and clean; the failure was a transient Stitch server rejection, not a malformed prompt. A later session's attempt may simply succeed.

2. **Review all new screens** in the Stitch UI (`projects/11542754846512650236`) and apply the design system if any drifted: `stitch.apply_design_system` with asset `assets/13836462354037157009` or edit with `stitch.edit_screens`.

3. **Decide on the 2-variant screens** (#31 error, #32 loading): pick the preferred variant; the others can be treated as dark/offline variants or deleted in the Stitch UI.

---

## Tooling (in `.planning/`)
- `mcp-connect.mjs` — generic MCP stdio bridge (`node .planning/mcp-connect.mjs stitch <tool> '<jsonParams>'`)
- `stitch-gen.mjs` — generate one screen from a prompt file → writes `/tmp/stitch_<slug>.json`
- `stitch-batch.mjs` — runs all prompts in `.planning/prompts/` sequentially (skips already-done slugs in `/tmp/stitch_manifest.json`)
- `stitch-manifest.json` — per-screen manifest (kept in sync by the generators)
- `prompts/26-…34-*.txt` — the new backend-grounded screen prompts

**MCP server config** used: `~/.cline/data/settings/cline_mcp_settings.json` (server name `stitch`).

**Background generation** (how this session ran the batch): the node runner spawns `stitch-gen.mjs` sequentially because each generation takes 30–45 s server-side. You can run it in the background and poll `/tmp/stitch_<slug>.json`.

---

## Backend↔UI alignment reminder (from the 08-05 handoff, unchanged)
- KYC tiers & limits (`application.yml`): Tier 1 ZMW 5,000 / 2,000 daily · Tier 2 ZMW 50,000 / 20,000 · Tier 3 ZMW 200,000 / 100,000 — reflected in `34-fee-disclosure` daily-limit numbers.
- Wallets created only after KYC approval — reflected in the empty-state KYC tip (`30-empty-state`).
- Currency ZMW, phone +260, reference `NWTXyyyyMMdd…` — used throughout the new screens.
- Fee endpoint `GET /fees/estimate?type=&amount=` — basis for `14-fee-estimate` and `34-fee-disclosure` (1% percentage fee example).
