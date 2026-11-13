# X08 · Transaction detail (admin console)

**Route:** `/transactions/:ref`  
**Canvas:** 1440 × 900 web, light and dark. Sidebar 248, content padding 32.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Investigate one transaction end to end.

## Layout
1. Details (amount, wallets, provider ref, idempotency key, created).
2. Ledger entries table (debits = credits check).
3. Timeline of every state change (Stage A/B/C, provider answers, auto checks).
4. Actions: Check with provider now, Mark failed + refund (danger).

## Behaviour
- Mark failed + refund needs a second admin and is allowed only after provider-confirmed failure or 24 h.

## Backend
- NEW `GET /api/v1/admin/transactions/{ref}` (with ledger + events), `POST …/requery`, `POST …/resolve` (maker-checker).

## Rules
- Matches the payout lifecycle in docs/architecture/payout-lifecycle.md.

## Prompt to give opencode
```text
Build admin screen X08 "Transaction detail" at route /transactions/:ref in the admin web app.
Read admin/docs/ADMIN_GUIDE.md and this BUILD.md first; open light.png and dark.png and match them.
Use the shared admin layout (sidebar, top bar, panel, data table, status text) and theme tokens only.
Implement the behaviour list, call the endpoints listed under Backend through the API client (mock any marked NEW until the backend step is done), check the user's role before showing actions, and write every action to the audit log endpoint.
Add tests (component + one end-to-end happy path). Run lint and tests, fix errors, list changed files and anything unfinished.
```
