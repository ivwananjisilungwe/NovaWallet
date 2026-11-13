# ADM-00 — Admin bootstrap, tooling, design system

**Area:** novawallet-admin · **Needs:** BE-99 and OPS-01

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Create `novawallet-admin/` (Vite + React + TS strict, Tailwind configured from the admin tokens, React Router, TanStack Query/Table, React Hook Form + Zod, Vitest + Testing Library, Playwright). ESLint/Prettier, `make gate`.
- Generate the TS API client from `docs/api/openapi.json`; env `VITE_API_BASE_URL`, `VITE_ENV` with a permanent SANDBOX/LIVE banner.
- Layout shell exactly as drawn in X02 (sidebar, header, user menu, environment badge); shared UI components listed in `_COMMON_ADMIN_RULES.md`; fonts from the same bundled assets.
- Build the fidelity tooling: Playwright harness + `npm run fidelity` using pixelmatch against the design PNGs; calibrate on the shell and document noise in `docs/admin/FIDELITY.md`. Prove it fails on a deliberate error.
- CI workflow (path filter `novawallet-admin/**`): lint, typecheck, unit, build, Playwright against the local sandbox backend.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
