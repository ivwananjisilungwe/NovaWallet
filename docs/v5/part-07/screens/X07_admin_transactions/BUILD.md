# X07 · Transactions (admin console)

**Route:** `/transactions`  
**Canvas:** 1440 × 900 web, light and dark. Sidebar 248, content padding 32.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Search all money movements.

## Layout
1. Chips by type and status; date/provider/amount filters.
2. Table: reference, customer, type, provider code, status, time, amount.

## Behaviour
- Click a row → X08.
- Export CSV (role: Finance and Super).

## Backend
- NEW `GET /api/v1/admin/transactions` with filters.

## Prompt to give opencode
```text
Build admin screen X07 "Transactions" at route /transactions in the admin web app.
Read admin/docs/ADMIN_GUIDE.md and this BUILD.md first; open light.png and dark.png and match them.
Use the shared admin layout (sidebar, top bar, panel, data table, status text) and theme tokens only.
Implement the behaviour list, call the endpoints listed under Backend through the API client (mock any marked NEW until the backend step is done), check the user's role before showing actions, and write every action to the audit log endpoint.
Add tests (component + one end-to-end happy path). Run lint and tests, fix errors, list changed files and anything unfinished.
```
