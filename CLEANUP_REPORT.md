# Cleanup Report — 13 Nov 2026

Original upload: `NovaWallet.zip` (≈ 249 MB unpacked, of which 117 MB was `docs.zip`, a duplicate of `docs/`). Cleaned size: see bottom.

## Kept untouched
- `novawallet-api/` source, tests, migrations (V1–V16), `pom.xml`, `mvnw`, `Dockerfile`, `docker-compose.yml`, `.env.example`, `SECURITY.md`, `.github/`, `.mvn/`, **`.git/` (history, branch, and all uncommitted Phase 1C Batch 1 changes)**, `docs/phase-1c/`, `docs/architecture/` (canonical per DEC BLK-03).
- Designs, unpacked: all v5 Parts 01–10 → `docs/v5/part-01…10` (originals are in your own archive; the zips were deleted from this tree).
- `TODO.md`, `reports/` (Phase 1B final reports, endpoint inventory, status report, test audit, PG16 verification).
- `docs/`: `SRS.md`, `AI-STRATEGY.md`, `CARD-ISSUING-RESEARCH.md`, `DIFFERENTIATOR-FEATURES.md`; `reference/COMPETITIVE-ANALYSIS.md`.

## Removed
| Item | Why |
|---|---|
| `docs.zip` (117 MB) | Exact duplicate of `docs/` |
| `docs/NovaWallet-v5-Part-0X-*.zip` (10 files) | Replaced by the unpacked `docs/v5/part-*` folders |
| `docs/CHATGPT_BUILD_GUIDE_BATCHES_1-4.md`, `docs/IMPLEMENTATION-PLAN.md`, `docs/NOVAWALLET_FINTECH_POLISH_PLAN.md`, `docs/UI-UX-RECOMMENDATIONS.md`, `docs/PROJECT-PLAN.md`, `docs/TECHNICAL-REPORT.md`, `docs/D1-RELEASE-READINESS-ASSESSMENT.md` | Aug–Oct 2026 documents about the **old** Flutter app/UI audit (that app is not in this upload and the v5 designs replace it); superseded by `prompts/` |
| `docs/architecture/*` (top-level copy) | Older duplicates of `novawallet-api/docs/architecture/*` (the API copies are newer and canonical) |
| `reports/PHASE_1C_*` (top-level) | Older copies of `novawallet-api/docs/phase-1c/*` (newer, canonical) |
| `reports/PHASE_1B_BATCH_2_REVIEW_CONSOLIDATION.md`, `PHASE_1B_BATCH_3_PLAN.md` | Superseded by the PHASE_1B_FINAL_* reports |
| `novawallet-api/report/` | Phase 0 / 1A / 1B-Batch-2 reports, historical. **They were tracked in git**, so `git status` will show them as deleted; they stay in git history. Run `git restore novawallet-api/report` if you want them back |
| `novawallet-api/target/` (9.7 MB build output), `novawallet-api/.idea/`, `novawallet-api/uploads/` | Build output, IDE settings, local upload folder (empty/KYC test dir) |

## Added
`StartHere.md`, this report, `AGENTS.md` (root) and AGENTS.md in `novawallet-api/`, `novawallet-app/`, `novawallet-admin/`, `infra/`; `docs/v5/SCREEN_INDEX.md` (generated from all 143 BUILD.md files: code, route, folder, endpoints); `docs/v5/DESIGN_START_HERE.md` (moved); placeholder READMEs for `docs/api`, `docs/providers`, `docs/app`, `docs/admin`; `prompts/` (the full ordered prompt pack).

## Things to know
- `docs/v5/part-06/fonts/` is **empty** in your designs; add Plus Jakarta Sans and Inter (see StartHere §4).
- Only `novawallet-api/` is a git repository. Decision DEC-24 (monorepo or separate repos) is raised in `S0-02`.
- Two files remain from the older era on purpose: `docs/reference/COMPETITIVE-ANALYSIS.md` (market context) and `SRS.md` (requirements; some content predates the v5 design — the designs and `prompts/` win where they differ).
cleaned size: 137M
