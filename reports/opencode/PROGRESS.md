# Build progress (for opencode / next agent)

Order: `prompts/01_MASTER_ORDER.md`. Decisions: `docs/DECISIONS.md`. Contract: `docs/api/CONTRACT_TARGET.md`. Update this file at the end of every prompt.

| # | Prompt | Status | Report | Notes |
|---|---|---|---|---|
| 1 | S0-01 commit batch1 & baseline | ✅ verified; **owner commit pending** | `S0-01_REPORT.md` | 316/316 tests; PG16 V1→V16 OK; recommend `git restore report/` |
| 2 | S0-02 workspace check & root CI | ✅ done; **owner git-root move pending** | `S0-02_REPORT.md` | DEC-24 = A (monorepo); actionlint not run (blocked) |
| 3 | S0-03 decisions & contract target | ✅ done | `S0-03_REPORT.md` | 123 endpoint rows: 27 exist, 10 ext, 5 conflict, 81 missing |
| 4 | BE-1C-2 authorization & ownership 🔒 | ✅ built + tested; **needs T1 independent review** | `BE-1C-2_REPORT.md` | Full suite 361/0/0/0 (2026-11-14) + gap test 5/0. Branch `feature/be-1c-2-authz` (pushed). SEC-13 recorded. Authz map `reports/PHASE_1C_AUTHZ_MAP.md` |
| 5 | BE-1C-3 financial concurrency 🔒 | 📝 plan written, awaiting owner approval | `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_3_PLAN.md` | needs DEC-04, DEC-11 |
| 6 | BE-1C-4 database integrity 🔒 | 📝 plan written, awaiting owner approval | `.../PHASE_1C_BATCH_4_PLAN.md` | V17 may be taken by BE-1C-3 |
| 7 | BE-1C-6 KYC/PII 🔒 | 📝 plan written, awaiting owner approval | `.../PHASE_1C_BATCH_6_PLAN.md` | DEC-08 BLOCKED; proposed DEC-35..40 not yet in DECISIONS.md |
| 8 | BE-1C-7 rate limiting 🔒 | 📝 plan written, awaiting owner approval | `.../PHASE_1C_BATCH_7_PLAN.md` | DEC-09 open; trusted-proxy list blocked on infra |
| 9 | BE-1C-8 audit/logging/errors 🔒 | 📝 plan written, awaiting owner approval | `.../PHASE_1C_BATCH_8_PLAN.md` | proposed decisions D2–D12 in the plan |

## Owner actions outstanding
1. Commit S0-01 (commands in `S0-01_REPORT.md`).
2. Move the git root to the monorepo (commands in `S0-02_REPORT.md`); move `prompts/NovaWallet/` and `Designs/` out first.
3. Put the fonts in `docs/v5/part-06/fonts/`.
4. Answer open decisions in `docs/DECISIONS.md`.

## Environment notes
- Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (default `java` is the 25 JRE and cannot compile).
- Docker works without sudo. PG16 check: `postgres:16` + `flyway/flyway:10` containers (fresh V1→head, upgrade prev→head, validate).
- Git (owner decision 2026-11-14): commit + push each finished, tested step to a per-prompt feature branch (current: `feature/be-1c-2-authz`). Never push to `main`; the owner merges. Commit only files you changed.
- Subagents: use the cheapest model. Parallel agents that run Maven need their own git worktree (one `target/` per tree).
- zsh: `cat` is aliased to batcat and prints nothing in agent shells; use `command cat`. `$VAR` lists don't word-split; use arrays.
- Plans for batches 3, 4, 6, 7, 8 were written early (in parallel) — re-verify their file:line evidence before implementing, since earlier batches will move code.
