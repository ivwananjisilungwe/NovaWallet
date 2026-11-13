# S0-02 — Workspace check (cleaned project) and root CI

**Area:** repo root, .github · **Needs:** S0-01 committed

---
Read root `AGENTS.md` and `StartHere.md`.

The project was cleaned on 13 Nov 2026 (see `CLEANUP_REPORT.md`): designs are already unpacked in `docs/v5/part-01..10`, `docs/v5/SCREEN_INDEX.md` exists, AGENTS files and `prompts/` are in place. Note: today only `novawallet-api/` is a git repository (remote: the owner's GitHub `NovaWallet` repo).

Do:
1. Verify the layout matches `StartHere.md`. List anything missing or unexpected. Do not delete anything.
2. **Repository decision (ask the owner, do not decide):** (A) one monorepo at the root containing api, app, admin, infra, docs, prompts; or (B) keep `novawallet-api` as its own repo and create separate repos for app and admin. Write the trade-offs in 6 lines and recommend A (one history, one CI, shared docs). Record as DEC-24 in `docs/DECISIONS.md` (created in S0-03) and STOP on this item until answered. If A: write the exact commands for the owner to move the git root safely (no history loss); do not run them.
3. `.editorconfig` and root `.gitignore` covering Java, Flutter, Node, IDEs, env files, keystores, `uploads/`, `target/`, `build/`.
4. Root `README.md`: what NovaWallet is, folder map, how to run the API locally, link to `StartHere.md` and `prompts/01_MASTER_ORDER.md`.
5. CI for the API (`.github/workflows/api-ci.yml` at the git root that will exist after the decision): JDK 17 Temurin, Maven cache, `./mvnw -B clean test`, upload surefire reports, path filter on `novawallet-api/**`. No deploy steps. Keep the existing workflow in `novawallet-api/.github` working until the move.
6. `docs/providers/README.md`: provider API documents (Lipila, SMS, bills aggregator, card partner) must be saved here by the owner; agents build only against documented contracts.
7. Fonts: `docs/v5/part-06/fonts/` is empty. Tell the owner to place the Plus Jakarta Sans and Inter font files (open-source, OFL licence) there; the app bundles them later.

Gates: `./mvnw -B clean test` still green; workflow linted (`actionlint`) or say it was not available.
Stop and report.
