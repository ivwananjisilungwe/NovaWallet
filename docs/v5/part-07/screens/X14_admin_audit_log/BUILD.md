# X14 · Audit log (admin console)

**Route:** `/audit`  
**Canvas:** 1440 × 900 web, light and dark. Sidebar 248, content padding 32.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Read-only record of every admin action.

## Layout
1. Filter chips; table: time, staff, action, target, IP.

## Behaviour
- Export for compliance.

## Backend
- NEW append-only audit table (no update/delete permission at DB level).

## Rules
- Keep for the legally required period.

## Prompt to give opencode
```text
Build admin screen X14 "Audit log" at route /audit in the admin web app.
Read admin/docs/ADMIN_GUIDE.md and this BUILD.md first; open light.png and dark.png and match them.
Use the shared admin layout (sidebar, top bar, panel, data table, status text) and theme tokens only.
Implement the behaviour list, call the endpoints listed under Backend through the API client (mock any marked NEW until the backend step is done), check the user's role before showing actions, and write every action to the audit log endpoint.
Add tests (component + one end-to-end happy path). Run lint and tests, fix errors, list changed files and anything unfinished.
```
