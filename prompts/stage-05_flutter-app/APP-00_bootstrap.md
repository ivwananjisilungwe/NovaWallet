# APP-00 — Flutter bootstrap, tooling, assets

**Area:** novawallet-app · **Needs:** BE-99, OPS-01 · **Design sources:** `docs/v5/part-06/` (fonts, tokens.json), `docs/v5/part-05/lottie`

---
Read root `AGENTS.md`, `novawallet-app/AGENTS.md`, `docs/v5/part-06/DESIGN_SYSTEM.md`.

Do:
1. `flutter create` in `novawallet-app/` (org `zm.novawallet`, application id `zm.novawallet.app`), Android first, keep ios/. Add the stack from AGENTS.md §4 with exact pinned versions listed in the report.
2. Folder structure from AGENTS.md §4. `analysis_options.yaml` strict (strict-casts/inference/raw-types, `very_good_analysis`-level rules, warnings = errors in CI).
3. **Flavors** `dev` / `staging` / `prod`: distinct application id suffixes, names ("NovaWallet Dev"), icons; config via `--dart-define-from-file=config/<flavor>.json` (`API_BASE_URL`, `FLAVOR`, `ENABLE_LOGGING`); commit `config/*.example.json` only; debug-only `X-Env` banner on non-prod.
4. **Assets:** copy from the design parts into `assets/` — fonts (`part-06/fonts`; if empty, STOP that item and tell the owner to add Plus Jakarta Sans and Inter), icons (SVG), illustrations, logos, category tiles, splash, Lottie JSON from part-05, with an `assets/README.md` mapping each asset to the screens that use it. Declare in `pubspec.yaml`. Keep original file names.
5. `bootstrap.dart`: error zone, `FlutterError.onError`, `ProviderScope`, placeholder home showing flavor.
6. Android: min/target SDK, release signing config reading `key.properties` (git-ignored), R8, `usesCleartextTraffic=false` (dev flavor may allow `10.0.2.2` via network security config), `allowBackup=false`, FLAG_SECURE plumbing placeholder.
7. `Makefile`/`tool/`: `format`, `analyze`, `gen`, `test`, `gate`, `fidelity`.
8. `.github/workflows/app-ci.yml` (path filter `novawallet-app/**`): the four gates + fidelity tests. No deploy.
9. `novawallet-app/README.md`: run per flavor against the local sandbox backend.

Acceptance: `make gate` passes; `flutter run --flavor dev` launches on an emulator and shows the placeholder; report Flutter/Dart versions.
