# X13 · Support cases (admin console)

**Route:** `/support`  
**Canvas:** 1440 × 900 web, light and dark. Sidebar 248, content padding 32.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Handle customer cases.

## Layout
1. Case list with status (Urgent, New, Waiting on provider, Waiting on customer).
2. Thread with linked transaction, quick replies, warning not to ask for PINs.

## Behaviour
- Assign, change status, close with resolution.

## Backend
- NEW support case + messages endpoints (or a helpdesk tool).

## Prompt to give opencode
```text
Build admin screen X13 "Support cases" at route /support in the admin web app.
Read admin/docs/ADMIN_GUIDE.md and this BUILD.md first; open light.png and dark.png and match them.
Use the shared admin layout (sidebar, top bar, panel, data table, status text) and theme tokens only.
Implement the behaviour list, call the endpoints listed under Backend through the API client (mock any marked NEW until the backend step is done), check the user's role before showing actions, and write every action to the audit log endpoint.
Add tests (component + one end-to-end happy path). Run lint and tests, fix errors, list changed files and anything unfinished.
```
