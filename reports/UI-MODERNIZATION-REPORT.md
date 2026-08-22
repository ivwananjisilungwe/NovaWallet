# NovaWallet UI Modernization Report

## Purpose

This report is the cumulative implementation log for the sequential effort to align the Flutter app with the Stitch designs while improving professionalism, accessibility, navigation, and full-stack readiness.

## User direction

> Make the Flutter app exactly like the Stitch designs or better, allow mock data, prepare the backend as needed, and make the app look and feel nice with strong navigation and UI/UX.

## Execution mode

Sequential execution: one todo/task at a time.

## Completed audits

### Flutter vs Stitch visual audit

- Verdict: **PARTIAL MATCH**, not pixel-perfect.
- `flutter analyze`: passed with two informational deprecations.
- `flutter test`: 170/170 passed.
- Detailed parity gaps were found in typography/font loading, login, onboarding, dashboard, send-money flow, bottom navigation, and several unverified screens.
- Exact runtime pixel equality remains unverified until deterministic Flutter screenshots are captured at the Stitch viewport sizes.

### Backend audit

- Verdict: **72/100 — conditionally releasable**.
- `./mvnw test`: 125 tests passed, 0 failures, 0 errors, 0 skipped.
- Important strengths include pessimistic wallet locking, atomic balance guards, fees, refresh rotation, PIN lockout, idempotency infrastructure, KYC limits, and audit records.
- Priority gaps include per-user idempotency-key scoping, email-verification policy before financial activity, authenticated change-password endpoint, balance-cache consistency, PostgreSQL-specific verification, and production operations evidence.

## Implementation progress

### Foundation phase

Status: **completed** (2026-08-09).

#### Task A — Inter font via GoogleFonts with test-safe fallback

- **File**: `novawallet-app/lib/core/theme/app_theme.dart`
- **Change**: Replaced hardcoded `fontFamily: 'Inter'` with `GoogleFonts.interTextTheme()` call, applying DESIGN.md typography overrides (headlineLarge, headlineMedium, titleLarge, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium) with explicit `fontWeight` and `letterSpacing`.
- **Behavior**: On device, `google_fonts` downloads Inter at runtime. In tests (no network), the font family falls back gracefully — the `TextTheme` still applies size/weight/spacing overrides, and widgets render with the device default sans-serif.
- **Evidence**: `flutter test` 170/170 passed; `google_fonts` prints expected stderr warnings in test environment (documented behavior, no test failures).

#### Task B — Centralized semantic/component tokens

- **File**: `novawallet-app/lib/core/theme/design_tokens.dart` (new file)
- **Content**: Three-layer token architecture:
  - **Primitive tokens**: raw color values from DESIGN.md (indigo scale, emerald, amber, rose, slate, white, black, dark-mode surfaces).
  - **Semantic tokens**: role-based colors (`primary`, `primaryVariant`, `secondary`, `success`, `warning`, `error`, `surface`, `onSurface`, `outline`, etc.).
  - **Component tokens**: `PillButton` sizing/spacing/shape, `BalanceCard` padding/shadow, `StatusChip` shape/colors, `Spacing` scale (4–96), `Shape` (4, 8, 12, 16, full), `Typography` scale (display-lg → caption-sm), `Motion` durations/easing, `Elevation` shadows.
- **Design decision**: Unused primitive tokens (dark-mode colors, tertiary/indigo shades) are retained intentionally for future screen adoption and dark-mode toggle — they surface as `unused_field` warnings but are not errors.

#### Task C — PillButton variants, accessibility, and migration

- **File**: `novawallet-app/lib/core/widgets/pill_button.dart`
- **Changes**:
  - Replaced `bool filled` with `PillButtonVariant` enum (`filled`, `outlined`, `text`).
  - Added `Semantics` wrapper with `label`, `button: true`, `enabled: !isDisabled`, and `hint: 'Loading'` when loading.
  - Enforced minimum 48×48 touch target (WCAG AA); expanded buttons use 56dp height.
  - Tokenized sizing via `ComponentTokens.pillButtonMinHeight`, `pillButtonPadding`, `pillButtonShape`, `pillButtonFontSize`, `pillButtonFontWeight`.
- **Bug fix**: `Semantics.disabled` does not exist in Flutter API — corrected to `Semantics.enabled: !isDisabled`.
- **Migration**: Updated 3 call sites (`dashboard_screen.dart`, `cards_screen.dart`, `admin_dashboard_screen.dart`) from `filled: true/false` to `variant: PillButtonVariant.filled/outlined`.

#### Verification results

| Command | Result |
|---|---|
| `dart format` | 7 files formatted (6 changed, 1 already clean) |
| `flutter analyze` | **0 errors**, 27 warnings (25 unused token fields — intentional library pre-population; 2 pre-existing `Radio` deprecations in `admin_dashboard_screen.dart` — outside scope) |
| `flutter test` | **170/170 passed** (0 regressions) |

#### Files changed

| File | Action |
|---|---|
| `lib/core/theme/app_theme.dart` | Modified — GoogleFonts Inter + DESIGN.md typography overrides |
| `lib/core/theme/design_tokens.dart` | **New** — three-layer token architecture |
| `lib/core/widgets/pill_button.dart` | Modified — variant enum, semantics, tokenization |
| `lib/core/widgets/widgets.dart` | Modified — internal `PillButton` calls migrated to variant API |
| `lib/features/wallet/screens/dashboard_screen.dart` | Modified — `filled:` → `variant:` migration |
| `lib/features/wallet/screens/cards_screen.dart` | Modified — `filled:` → `variant:` migration |
| `lib/features/admin/screens/admin_dashboard_screen.dart` | Modified — `filled:` → `variant:` migration |

#### Limitations

- `google_fonts` cannot load Inter in the test environment (no network/font assets). This is expected and documented; the fallback chain prevents test failures.
- 25 `unused_field` warnings in `design_tokens.dart` are intentional — tokens are pre-populated for dark mode and future screen adoption.
- 2 `deprecated_member_use` info-level findings in `admin_dashboard_screen.dart` (`Radio.groupValue`/`onChanged`) are pre-existing and outside the foundation scope.

No backend files were changed. No new dependencies were added.

#### Independent orchestrator verification

The orchestrator reran the project gates after the implementation agent completed the phase:

| Command | Result |
|---|---|
| `flutter analyze` | Completed with **27 non-fatal findings**: 25 unused private token fields and 2 pre-existing deprecated `Radio` API notices. No analyzer errors. Because the command exits non-zero for findings, tests were rerun separately. |
| `flutter test --reporter compact` | **170/170 passed**, 0 failures. The run emitted repeated `google_fonts` binding/font-loading warnings from non-widget test contexts, but these did not fail tests. |

The `google_fonts` warnings mean the current runtime font strategy is not yet suitable for deterministic golden testing. Before golden baselines are accepted, Inter should be bundled as local Flutter font assets or the test harness must initialize/configure Google Fonts deterministically. No external font files were downloaded during this phase.

No backend edits or commits were made by the orchestrator for this task.

## Auth/onboarding phase status

Status: **completed (2026-08-10)** — see the verified completion section below. The earlier "not completed" entry was stale: the screens had been rewritten after the last report update but were unverified and unreported. The 2026-08-10 scan reproduced 3 failing auth tests, had them fixed, and reran all gates to green (177/177).

## Next task

Dashboard and wallet parity — the next sequential slice per the phased plan. Do not advance beyond it until a dated completion section with actual file changes and test results is appended here.

---

## Login and Onboarding Slice — Completed

**Date:** 2026-08-10
**Slice:** Auth/onboarding visual parity — verified completion.

### Files changed

| File | Action |
|---|---|
| `lib/features/auth/screens/login_screen.dart` | Modified — remember-me row overflow fix (`Flexible` + ellipsis on the label), `?suffix` null-aware element (lint), no provider/behavior change |
| `lib/features/auth/screens/onboarding_screen.dart` | No change needed — screen already met requirements; navigation-test failures were test-viewport issues |
| `test/features/auth/screens/login_onboarding_screen_test.dart` | Modified — `ensureVisible` before tapping CTAs in the 2 navigation tests; `_` wildcard lint cleanup |

### Design and architecture decisions

- **Overflow fix (real bug):** the "Remember me / Forgot password?" row overflowed by 119 px at 375 px width in the test font. The checkbox+label group is now wrapped in `Flexible` and the label ellipsizes when space is tight, while the "Forgot password?" TextButton keeps its natural width. This fixes narrow devices and large accessibility text scales, not just tests.
- **Test-side navigation fix (correct behavior):** the screen is intentionally scrollable (responsive-layout requirement), so the CTAs sit below the fold in the default 800×600 test viewport. The tests now scroll to the CTA (`ensureVisible`) before tapping, mirroring a real user.
- No new dependencies. No Google OAuth invented. `authProvider` login behavior and `ApiException` handling preserved.

### Tests and commands actually run

| Command | Result |
|---|---|
| `flutter test test/features/auth/screens/login_onboarding_screen_test.dart` | **7/7 passed** |
| `flutter analyze` | **0 errors** — 27 findings, all the documented baseline (25 intentional unused token fields + 2 pre-existing Radio deprecations); zero findings in the touched files |
| `flutter test --reporter compact` | **177/177 passed** (174 pre-existing + 3 previously failing) |

RED state captured before the fix: 3 failures — 2 navigation tests (tap offset `Offset(400.0, 612.0)` off-screen, outside the 800×600 root bounds) and 1 overflow test (`A RenderFlex overflowed by 119 pixels on the right` at `login_screen.dart:117`).

### Known limitations

- The remember-me label ellipsizes only below ~290 px of row width; a `FittedBox`/wrap fallback could later give zero-ellipsis rendering at all text scales (out of scope for this slice).
- `google_fonts` binding/font warnings in non-widget test contexts remain (expected, documented; not test failures). Golden baselines still not accepted until font loading is deterministic or Inter is bundled locally.
- Screens still not compared pixel-perfect against Stitch viewport sizes (requires deterministic screenshots).

### Next sequential task

Dashboard and wallet parity.
