# S0-02 — Workspace check and root CI

Date: 2026-11-13

## 1. Layout vs `StartHere.md`
Present as described: `novawallet-api/` (git repo), `novawallet-app/`, `novawallet-admin/`, `infra/` (AGENTS.md only), `docs/` (SRS, AI-STRATEGY, CARD-ISSUING-RESEARCH, DIFFERENTIATOR-FEATURES, reference/, providers/, v5/part-01..10, SCREEN_INDEX.md), `reports/`, `prompts/`, `TODO.md`, `AGENTS.md`, `CLEANUP_REPORT.md`.

**Unexpected (not deleted; owner decides):**
- `Designs/` (117 MB): the 10 original design zips. These are already unpacked in `docs/v5`. Now git-ignored via `Designs/*.zip`.
- `prompts/NovaWallet/` (137 MB): a second copy of the project (AGENTS.md, StartHere.md, TODO.md, docs/, `infra/ci.yml`) nested inside `prompts/`. Looks like a packaging leftover. **Recommend deleting it or moving it out of the repo before the first monorepo commit**, otherwise it adds 137 MB of duplicates.
- `docs/v5/part-06/fonts/` contains only `PLACE_FONTS_HERE.txt`. Fonts are missing (see 7).

## 2. DEC-24: repository layout
**Owner decision (2026-11-13): A, one monorepo at `NovaWallet/`.**
| | A: monorepo | B: separate repos |
|---|---|---|
| History | one; API history kept | split across repos |
| CI | one pipeline set with path filters | per-repo, contract changes span repos |
| Docs/prompts | live beside code | need their own home |
| Cost | one move now | ongoing sync overhead |

### Owner commands (run after the S0-01 commit; no history loss, no rewrite)
Moving `.git` up one level keeps every SHA. Git then sees the API files as renamed into `novawallet-api/`, and `git log --follow` still traces them.
```bash
cd ~/DEVELOPMENT/PROJECTS/NovaWallet
git -C novawallet-api status --short        # must be clean (S0-01 committed)
cp -a novawallet-api/.git ../novawallet-api.git.backup   # safety copy
mv novawallet-api/.git .git
# decide on the duplicates first (section 1), e.g.:
#   mv prompts/NovaWallet ../NovaWallet-prompt-copy
git status --short | head -50               # expect: API files as renamed, new root files untracked
git add -A
git status --short | grep -vE '^(R|A|M) ' | head    # review anything odd
git diff --cached --stat | tail -5          # sanity: no Designs/*.zip, no target/, no .env
git commit -m "chore: move git root to monorepo (DEC-24 A)"
git push                                     # same remote, same branch
rm -rf ../novawallet-api.git.backup          # only after the push is verified
```
After the move, `novawallet-api/.github/workflows/ci.yml` stops running (GitHub reads only the root `.github/`). The root `api-ci.yml` replaces its test job. Its **Docker image build/push job is not carried over** on purpose (no deploy steps in this prompt); OPS-03 rebuilds it. Delete the old file in OPS-03, not now.

## 3–6. Files added
- `.editorconfig`
- `.gitignore` (root): Java, Maven, Flutter/Dart, Node/Vite, IDEs, env files, keystores/keys, `uploads/`, `target/`, `build/`, logs, `Designs/*.zip`
- `README.md` (root): what it is, folder map, how to run the API, links to StartHere, the master order and PROGRESS
- `.github/workflows/api-ci.yml`: JDK 17 Temurin, Maven cache, secret scan, `./mvnw -B clean test`, surefire upload, path filter `novawallet-api/**`, no deploy
- `docs/providers/README.md`: expanded (per-provider folders, status, no-keys rule)

## 7. Fonts — owner action
Put the **Plus Jakarta Sans** and **Inter** font files (OFL, from Google Fonts) into `docs/v5/part-06/fonts/`. The app's pixel-fidelity checks cannot pass without them.

## Report — S0-02
Done: layout check; DEC-24 answered (A) with move commands; .editorconfig; root .gitignore; README; root API CI; providers README.
Files changed: 6 (`.editorconfig`, `.gitignore`, `README.md`, `.github/workflows/api-ci.yml`, `docs/providers/README.md`, this report)
Evidence: no API source changed, so the S0-01 run still applies (316/0/0/0, 2026-11-13). **actionlint not run**: not installed, and running its Docker image was blocked by the local permission policy. Workflow syntax reviewed by hand only.
Blocked / not done: actionlint; the git-root move (owner-only); fonts (owner).
Decisions needed: delete or move `prompts/NovaWallet/` and `Designs/` before the monorepo commit (recommend: move both outside the repo).
Risks noticed (not fixed): the old API pipeline's Docker publish stops after the move until OPS-03.
Next: S0-03.
