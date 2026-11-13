# Common rules for every ADM prompt

Read `novawallet-admin/AGENTS.md` fully. The admin console is built **exactly like the designs** in `docs/v5/part-07/` (16 screens X01–X16; PNG/SVG light+dark, BUILD.md each) and `docs/v5/part-07/docs/ADMIN_GUIDE.md`.

For every screen in the prompt:
1. Open its folder via `docs/v5/SCREEN_INDEX.md`: BUILD.md, SVG text (exact sizes, colours, copy), PNGs. Admin tokens come from the Part 07 docs/`part-06/tokens.json`; no ad-hoc colours.
2. Build with shared components in `src/ui/` (table, filters bar, status chip, detail drawer, confirm dialog with reason field, stat tile, empty/loading/error states). Add missing ones first.
3. Every state in BUILD.md plus loading/empty/error/forbidden (403 view) per ADMIN_AGENTS rules; permission-gated UI **and** server enforcement already exists (BE-1D-07) — test both ways with different roles.
4. Data from the generated TypeScript client (`openapi-typescript`/`orval` from `docs/api/openapi.json`); no hand-written URLs scattered in components.
5. **Fidelity gate:** Playwright screenshot tests at the design viewport(s) stated in the Part 07 README for light/dark, seeded sandbox data equal to the design's sample data; `npm run fidelity -- <CODE>` compares to the design PNG with pixelmatch (≤ 3 % differing pixels, no structural difference); diffs in `playwright-report/fidelity/`; unavoidable deviations logged in `docs/admin/FIDELITY_LOG.md`.
6. Tests: component/unit (Vitest), role-matrix tests, e2e per flow (Playwright) against the sandbox backend with seeded staff for each role.
7. Gates from ADMIN_AGENTS.md, a11y checks (axe in Playwright), report with the fidelity table. Stop. No commit.
