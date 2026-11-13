# APP-01 — Design system (`Nw` components) + fidelity tooling

**Area:** novawallet-app · **Needs:** APP-00 · **Design sources:** `docs/v5/part-0N/docs/COMPONENTS.md` (read all of them), `docs/v5/part-06/tokens.json`, `DESIGN_SYSTEM.md`, `docs/v5/part-05/MOTION.md`

---
Read `novawallet-app/AGENTS.md` fully.

**Part A — theme & components**
1. `AppColors` `ThemeExtension` (light + dark) with every token in COMPONENTS.md (bg, surface, surface2/3, border/borderStrong, text1-3, primary, textOnPrimary, primarySoft/Text, gold*, success*, warning*, danger*, info*, hero gradient, chart + category colours from `tokens.json`). Typography scale (Plus Jakarta Sans for headings/numbers with tabular figures, Inter body), radii (input 18, card 24, sheet 34, hero 36, pill), spacing, button heights, canvas constants. Theme mode system/light/dark persisted.
2. Implement **every shared component named in the COMPONENTS.md files** (read parts 01, 02, 03, 04, 09 — they may add components) as `Nw…` widgets in `core/ui/`, with the exact measurements given. Each has: a `const`-friendly API, Semantics, loading/disabled/error variants where described, a golden at 390 wide in light/dark.
3. Icons: map the design's icon set (SVG in `assets/icons`) to an `NwIcon` enum; category tiles to `NwCategoryTile`; illustrations to `NwIllustration`; logos; Lottie wrapper `NwLottie` honouring reduce-motion.
4. `Money` type (ngwee, ZMW) and formatters exactly as the designs print amounts ("K 1,250.00"; check each sample), phone formatter (+260 97… masking as drawn), date/time formatters (Africa/Lusaka; "Today", "Yesterday", "12 Nov" as drawn).
5. Debug-only **component catalog** screen (all components × states × light/dark × text scale).

**Part B — fidelity tooling** (build it carefully; every later prompt depends on it)
6. `test/fidelity/fidelity_harness.dart`: pumps a widget in a `MaterialApp` at 390×844, DPR 3.0, real bundled fonts loaded (FontLoader), fixed clock, animations disabled, system status bar painted like the design if the design shows it, renders to PNG (`build/fidelity/<CODE>_<theme>.png`, 1170×2532).
7. `tool/fidelity_compare.dart <CODE|--all-changed>`: finds the design PNG via `docs/v5/SCREEN_INDEX.md`, loads both, reports (a) % of pixels differing beyond a small colour tolerance, (b) a structural check by comparing downscaled blocks to localise errors, and writes a side-by-side + diff heatmap PNG to `build/fidelity/diff/`. Exit non-zero above 3 %. Configurable ignore regions (status bar, system keyboard) listed per screen in `test/fidelity/ignore_regions.yaml`.
8. Calibrate on 5 simple components/screens (e.g. A01 splash, a button sheet) and write the measured baseline noise in `docs/app/FIDELITY.md` plus how to read the diff.
9. A fidelity test for each component golden compared against the design where a drawn instance exists (e.g. hero card on E02).

Acceptance: gates pass; catalog runs; the compare tool demonstrably fails on a deliberately wrong colour/size and passes on a correct build (include both diffs).
