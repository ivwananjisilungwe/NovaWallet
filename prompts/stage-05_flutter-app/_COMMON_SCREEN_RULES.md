# Common rules for every APP prompt that builds screens

You are building screens **exactly** as designed. Before anything else read `novawallet-app/AGENTS.md` (sections 2, 3, 5, 6) in full.

For every screen code listed in the prompt:

1. **Open its folder** via `docs/v5/SCREEN_INDEX.md`: `BUILD.md`, `light.svg`, `dark.svg`, `light.png`, `dark.png`. View the PNGs. Read the SVG text for exact coordinates, sizes, colours, radii, strings. Copy copy-text verbatim (including punctuation, currency format, example names/amounts for fixtures).
2. Write a one-paragraph **build note** per screen in `docs/app/screens/<CODE>.md`: components used, measurements taken from the SVG, states, navigation (from BUILD.md "Navigation" table), data/endpoints, any thing not drawn and how you composed it.
3. Build the screen from `Nw` components in `lib/core/ui`. Missing component → add it to `core/ui` first (documented in `docs/v5/part-0X/docs/COMPONENTS.md` terms), with its own fidelity-tested golden.
4. **States:** build every state the folder shows (e.g. F03 over limit, F05 waiting, F07 failed, F08 pending are separate designs of one flow) plus loading/error/offline per `AGENTS.md` §6. Each drawn variant gets a fidelity test.
5. Wire to real data through repositories (generated API client from `docs/api/openapi.json`). Until BE-99 is done everything runs against the sandbox backend (`infra/compose/local.yml`) — sandbox magic values are in `docs/api/SANDBOX.md`. No mock data in screens.
6. Routing: use the exact route from BUILD.md; guards (auth, PIN lock, KYC level, feature flags from app config) as BUILD.md says; deep-link targets declared.
7. **Fidelity gate (mandatory):** `test/fidelity/<CODE>_test.dart` for light and dark at 390×844 @3x, with the design's sample data; run `dart run tool/fidelity_compare.dart <CODE>`; ≤ 3 % pixel difference and no structural difference. Attach the diff percentages to the report. Record unavoidable deviations in `docs/app/FIDELITY_LOG.md` for owner approval.
8. Widget tests: every state + validation + navigation. Integration test per flow (see prompt). Accessibility: semantics labels, 48dp targets, text scale 1.3/2.0 without clipping (scroll instead).
9. Run the app on an emulator against the sandbox backend, walk the flow, and report what you saw (or say you could not run it).
10. Gates from `AGENTS.md` §7, then the report (root format + fidelity table). Stop. No commit.
