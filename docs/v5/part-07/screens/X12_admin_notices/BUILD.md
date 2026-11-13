# X12 · Notices (admin console)

**Route:** `/notices`  
**Canvas:** 1440 × 900 web, light and dark. Sidebar 248, content padding 32.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Write in-app banners and push messages.

## Layout
1. Form: internal title, customer message, level (Info/Warning/Critical), where to show (Home banner, Top up screen, Push), audience.
2. Live phone preview.

## Behaviour
- Publish now or schedule; expire automatically.

## Backend
- NEW notices endpoints; app reads `/app/notices`.

## Rules
- Keep messages short and calm; never blame customers.

## Prompt to give opencode
```text
Build admin screen X12 "Notices" at route /notices in the admin web app.
Read admin/docs/ADMIN_GUIDE.md and this BUILD.md first; open light.png and dark.png and match them.
Use the shared admin layout (sidebar, top bar, panel, data table, status text) and theme tokens only.
Implement the behaviour list, call the endpoints listed under Backend through the API client (mock any marked NEW until the backend step is done), check the user's role before showing actions, and write every action to the audit log endpoint.
Add tests (component + one end-to-end happy path). Run lint and tests, fix errors, list changed files and anything unfinished.
```
