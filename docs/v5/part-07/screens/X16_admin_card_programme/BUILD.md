# X16 · Card programme (admin console)

**Route:** `/cards`  
**Canvas:** 1440 × 900 web, light and dark. Sidebar 248, content padding 32.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Track the virtual card launch.

## Layout
1. KPI cards (waitlist, issued, spend, declines).
2. Launch checklist with status.
3. Waitlist table.

## Behaviour
- Becomes the card operations page once the issuer is live.

## Backend
- Waitlist endpoint now; issuer integration later (see virtual cards doc).

## Prompt to give opencode
```text
Build admin screen X16 "Card programme" at route /cards in the admin web app.
Read admin/docs/ADMIN_GUIDE.md and this BUILD.md first; open light.png and dark.png and match them.
Use the shared admin layout (sidebar, top bar, panel, data table, status text) and theme tokens only.
Implement the behaviour list, call the endpoints listed under Backend through the API client (mock any marked NEW until the backend step is done), check the user's role before showing actions, and write every action to the audit log endpoint.
Add tests (component + one end-to-end happy path). Run lint and tests, fix errors, list changed files and anything unfinished.
```
