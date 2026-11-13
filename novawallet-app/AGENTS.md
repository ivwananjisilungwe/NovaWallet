# AGENTS.md — novawallet-app (Flutter)

Read the root `AGENTS.md` first, then all of this file before every task. If a prompt conflicts with this file, stop and ask.

## 1. What this is
The NovaWallet mobile app (Android first, Google Play). It talks to `novawallet-api` at `/api/v1`. The backend is finished before this app is built; the frozen contract is `docs/api/openapi.json` and `docs/api/CONTRACT.md`.

**The design is not a suggestion. The app must look and behave exactly like the v5 "Horizon" designs.**

## 2. Design sources (the truth, in this order)
1. `docs/v5/SCREEN_INDEX.md` — every screen code (A01 … Q13), route, folder, endpoints.
2. The screen folder: `BUILD.md` (layout, sizes, copy, states, navigation, data), `light.png` + `dark.png` (3×, 1170 × 2532), `light.svg` + `dark.svg` (**open the SVG as text: it has the exact coordinates, sizes, colours, radii and strings**).
3. `docs/v5/part-0N/docs/COMPONENTS.md` — shared components (`Nw…` names) and theme tokens.
4. `docs/v5/part-06/tokens.json` + `DESIGN_SYSTEM.md` — colours, gradients, type, radius, spacing; icons, illustrations, logos, category tiles, app icons, splash.
5. `docs/v5/part-05/MOTION.md` + `lottie/` — animations.
If two sources disagree: PNG/SVG wins for visuals, BUILD.md wins for behaviour. Report the conflict.

## 3. Hard rules
1. No git writes. Never touch `novawallet-api/` or `novawallet-admin/`.
2. **Never invent API shapes.** Use the generated client from `docs/api/openapi.json`. Missing endpoint → `BLOCKED:` + repository interface with a debug-only fake. Never call an endpoint that does not exist.
3. **Never redesign.** No new colours, spacing, icons, copy, layouts or "improvements". Use the exact strings from BUILD.md/SVG. If a state is not drawn, compose it from COMPONENTS.md and the nearest drawn state, and list it in the report.
4. No hardcoded colour, size, radius or text style in screens — only theme tokens and `Nw` components from `lib/core/ui/`.
5. Money is never a `double`: integer ngwee / `Money` type; format at the UI edge ("K 1,250.00").
6. No secrets in the repo; config by `--dart-define-from-file`. No PIN/token/password in logs or storage (tokens only in `flutter_secure_storage`).
7. No weakening security to make something work (no cleartext, no bad-certificate callbacks).
8. A task is done only when section 7 gates pass and the fidelity check (section 5) passes for every screen in the prompt.

## 4. Stack & structure
Flutter stable, Dart 3 · Riverpod (generator) · go_router (typed routes = the routes in BUILD.md) · dio (+ auth, refresh-once, idempotency, redacted logging, error mapping) · freezed/json_serializable · drift (non-sensitive cache) · flutter_secure_storage · local_auth · lottie · flutter_svg · intl/ARB.

```
lib/
  app/        bootstrap, router, flavors, shell
  core/       config, network, error, storage, money, logging
  core/ui/    theme (AppColors ThemeExtension), tokens, Nw components   ← from COMPONENTS.md
  features/<flow>/{data,domain,presentation}
  l10n/
assets/       fonts, icons, illustrations, lottie (copied from docs/v5/part-05, part-06)
test/fidelity/ goldens + design comparison
```
Dependency direction: presentation → domain ← data. `core/ui` imports no feature.

## 5. Design fidelity protocol (every screen)
1. Read the screen's `BUILD.md` and SVG (both themes). Write down the measurements you will use.
2. Build it from `Nw` components. If a needed component is missing, add it to `core/ui` per COMPONENTS.md first.
3. Add `test/fidelity/<CODE>_test.dart`: pump the screen at **390 × 844 logical, devicePixelRatio 3.0**, real fonts loaded, fixture data equal to the sample data shown in the design (same names, amounts, dates), light and dark. Save `build/fidelity/<CODE>_{light,dark}.png` (1170 × 2532).
4. Run `dart run tool/fidelity_compare.dart <CODE>`: it compares your PNG with the design PNG and writes a diff image + percentage. **Target: ≤ 3 % differing pixels and no structural difference** (position, size, colour, radius, missing/extra element). Font anti-aliasing noise is the only acceptable difference.
5. Look at the diff image. Fix until it passes. If something cannot match (platform status bar, real system keyboard), record it in `docs/app/FIDELITY_LOG.md` with the reason. The owner approves every logged deviation.
6. Then check responsiveness without changing the 390 layout: 360 and 412 widths, text scale 1.3 and 2.0 (no clipping, scroll instead), small height 640.
7. Report a table: code · light % · dark % · states built · deviations logged.

## 6. UX standards (in addition to what BUILD.md says)
- **States:** loading (the designed skeletons P07/P08), empty, error (P04), offline (P01/P02 + cached data), success. Missing state = not done.
- **Money flows:** amount → recipient → review (amount, fee, total, name) → PIN → result. One `Idempotency-Key` per attempt, reused on retry. Unknown outcome uses the designed "Payment status unknown" (P03) / pending screens — never show failed, never invite a blind retry.
- **Forms:** validate on blur + submit, right keyboard, autofill hints, disabled while submitting, keep input on error, Zambian phone formats (+260, MTN/Airtel/Zamtel).
- **Accessibility:** 48dp targets, Semantics labels (amounts read as words), contrast AA, reduce-motion respected, TalkBack order sane.
- **Performance:** 60 fps on a 2 GB Android, const widgets, builders for lists, cold start first frame < 2 s.
- **Trust:** hide-balance (E03), FLAG_SECURE on PIN/KYC/card screens, auto-lock (C02), session expired (C06), masked phone/ID numbers.
- **Low bandwidth:** bundled fonts/assets, small payloads, cancel on leave, backoff retries for reads only.

## 7. Gates
```
dart format --set-exit-if-changed .
flutter analyze                                   # zero issues
dart run build_runner build --delete-conflicting-outputs
flutter test --coverage                           # all green, incl. test/fidelity
dart run tool/fidelity_compare.dart --all-changed # every screen in this prompt passes
```
Coverage: 80 % domain/core, 60 % overall, 100 % money formatting and status mapping. Each screen: widget test per state + fidelity test both themes. Run the app on an emulator against the dev/staging API, walk the screens, and say what you saw. If you could not run it, say so.

## 8. Report
Root report format, plus the fidelity table (section 5.7), endpoints used, and `BLOCKED:` items.
