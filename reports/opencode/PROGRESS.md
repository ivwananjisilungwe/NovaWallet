# Build progress (for opencode / next agent)

Order: `prompts/01_MASTER_ORDER.md`. Decisions: `docs/DECISIONS.md`. Contract: `docs/api/CONTRACT_TARGET.md`. Update this file at the end of every prompt.

| # | Prompt | Status | Report | Notes |
|---|---|---|---|---|
| 1 | S0-01 commit batch1 & baseline | ✅ verified; **owner commit pending** | `S0-01_REPORT.md` | 316/316 tests; PG16 V1→V16 OK; recommend `git restore report/` |
| 2 | S0-02 workspace check & root CI | ✅ done; **owner git-root move pending** | `S0-02_REPORT.md` | DEC-24 = A (monorepo); actionlint not run (blocked) |
| 3 | S0-03 decisions & contract target | ✅ done | `S0-03_REPORT.md` | 123 endpoint rows: 27 exist, 10 ext, 5 conflict, 81 missing |
| 4 | BE-1C-2 authorization & ownership 🔒 | 🔧 **in progress**: code mostly done, tests NOT run | `HANDOFF.md` | DEC-03 = single ADMIN; plan at `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_PLAN.md` |

## Owner actions outstanding
1. Commit S0-01 (commands in `S0-01_REPORT.md`).
2. Move the git root to the monorepo (commands in `S0-02_REPORT.md`); move `prompts/NovaWallet/` and `Designs/` out first.
3. Put the fonts in `docs/v5/part-06/fonts/`.
4. Answer open decisions in `docs/DECISIONS.md`.

## Environment notes
- Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (default `java` is the 25 JRE and cannot compile).
- Docker works without sudo. PG16 check: `postgres:16` + `flyway/flyway:10` containers (fresh V1→head, upgrade prev→head, validate).
- Agents never write to git; the owner commits.
