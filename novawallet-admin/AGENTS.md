# AGENTS.md — novawallet-admin (staff console)

Read the root `AGENTS.md` first. Designs: `docs/v5/part-07/` (README, docs/ADMIN_GUIDE.md, screens X01–X16 each with BUILD.md). Match them.

## Stack (from the Part 07 spec)
Vite + React + TypeScript (strict), Tailwind with the admin design tokens, React Router, TanStack Query (server state), TanStack Table (tables), React Hook Form + Zod (forms), Vitest + Testing Library (unit/component), Playwright (e2e). Env: `VITE_API_BASE_URL`, `VITE_ENV=SANDBOX|LIVE` (show a permanent coloured environment banner).

## Rules
1. **Staff only, least privilege.** Every route and action is permission-gated in the UI **and** enforced by the API. UI hiding is never the security control.
2. **Auth:** email + password + mandatory TOTP (first sign-in forces TOTP setup). Short sessions, idle timeout (15 min), re-auth (password/TOTP) for sensitive actions: freeze/unfreeze, KYC decisions, fee/limit changes, refunds/adjustments, role changes.
3. **Four-eyes** where the backend supports it (fee changes, manual adjustments, role grants): maker submits, checker approves; the same person cannot do both.
4. **Every mutating action** asks for a reason (stored in audit log), shows a confirmation with the exact effect, and is idempotent.
5. **PII:** masked by default; reveal requires permission + reason and is audited. KYC documents viewed in-app only, never downloaded to disk unless permission allows; no caching of documents.
6. **Tables:** server-side pagination/filter/sort, URL-synced filters, CSV export only where permitted (and audited), empty/loading/error states.
7. **No business logic in the UI.** Amounts formatted from minor units; never compute balances client-side.
8. **Security headers / CSP** in the hosting config, no inline scripts, no tokens in localStorage (prefer httpOnly cookie session if the backend supports it; otherwise memory + refresh with strict handling — record the decision).

## Gates
```
npm run lint && npm run typecheck && npm run test -- --coverage && npm run build
npx playwright test   # against a local API with seeded sandbox data
```
Accessibility: keyboard navigable, visible focus, labels, contrast AA. Report what you verified in a browser.

## Never
Touch `novawallet-api/` (except when a prompt explicitly includes a backend part), store secrets in the bundle, call provider APIs directly from the browser.
