# NovaWallet v5 — Admin console guide and ChatGPT instructions

The admin console is a **separate web app** for NovaWallet staff. It has 16 screens (X01–X16), each in light and dark, and uses the same Horizon minimal style as the mobile app. Every screen has its own folder in `screens/` with `light.png`, `dark.png`, both SVGs and a `BUILD.md`.

## 1. What your backend already has

From the repo scan:

- **Admin controllers:** `AdminUserController`, `AdminWalletController` and `AdminKycController`, all protected by one role, `hasRole('ADMIN')`. `FeeController` has admin-only methods.
- **Audit:** there is an `audit` package. Extend it; don't create a second one.
- **Missing:** separate admin authentication, more than one admin role, maker-checker (two-person approval), admin transaction search, reconciliation views, provider switches, notices, support cases and metrics.

## 2. Tech choice (recommended)

| Part | Choice | Why |
|---|---|---|
| Admin web app | **React + TypeScript + Vite** | The most common admin stack; easiest for opencode and for hiring. |
| UI | Tailwind CSS + a small component set built from `ADMIN_TOKENS` (below) | Matches the design exactly; no heavy UI kit. |
| Data | TanStack Query (server state), TanStack Table (tables), React Router | Caching, pagination and filters done right. |
| Charts | Recharts | Simple line and area charts. |
| Auth | Admin JWT in an httpOnly cookie, TOTP 2-step | Staff tokens are never stored in localStorage. |
| Hosting | Separate domain (e.g. `admin.novawallet.zm`) behind VPN or an IP allowlist | Admin should never be reachable from the open internet. |

*Alternative:* Flutter Web, if you want one language. It is slower for big data tables and less common for back-office tools. The designs work for either.

### Admin tokens (same values as the app)

- Background: light #F3F5FB, dark #060A18. Surface: #FFFFFF / #0E1430. Surface2: #ECF0F8 / #151D3D.
- Primary: #2F5BF5 / #7896FF. Status colours, text colours and hero gradient are the same as in `docs/COMPONENTS.md`.
- Layout: sidebar 248, content padding 32, panels radius 20, table row height 52–64, header row 40 with 11 px uppercase labels.
- Fonts: Plus Jakarta Sans for headings and numbers, Inter for body text.

## 3. Security rules the admin must follow

1. **Separate admin login.** Use a different JWT audience/issuer from customers, 15-minute access tokens, TOTP required, lockout after 5 failures.
2. **Roles (RBAC):** SUPPORT, KYC_REVIEWER, FINANCE, SUPER_ADMIN. See the matrix on X15. Replace the single `hasRole('ADMIN')` with permission checks.
3. **Maker-checker:** refunds or mark-failed, limit and fee changes, ledger adjustments, and KYC "suspected fraud" all need a second person to approve.
4. **No balance editing.** Money is only corrected through ledger adjustment entries.
5. **Audit everything:** views of customer data, every change, sign-ins and exports. The audit table is append-only.
6. **PII:** phone numbers are masked by default, NRC images are served through short-lived signed URLs, and exports are limited to Finance and Super admin.
7. **Environment badge:** SANDBOX (amber) or LIVE (red) is always visible.

## 4. Screen list

| ID | Screen | Route | Main backend need |
|---|---|---|---|
| X01 | Staff sign in | `/login` | Admin auth + TOTP (NEW) |
| X02 | Dashboard | `/` | Metrics + alerts (NEW) |
| X03 | Users | `/users` | AdminUserController (extend) |
| X04 | User detail | `/users/:id` | AdminUser + AdminWallet freeze |
| X05 | KYC review queue | `/kyc` | AdminKycController |
| X06 | KYC review detail | `/kyc/:id` | Approve/reject with reason codes |
| X07 | Transactions | `/transactions` | Admin transaction search (NEW) |
| X08 | Transaction detail | `/transactions/:ref` | Ledger + events, requery, resolve (NEW) |
| X09 | Reconciliation | `/reconciliation` | Phase 1B Batch 3 reconciliation (NEW) |
| X10 | Providers | `/providers` | Provider routes kill switch (NEW) |
| X11 | Limits and fees | `/settings/limits` | Fee API + limits config + change requests |
| X12 | Notices | `/notices` | Notices (NEW) |
| X13 | Support cases | `/support` | Cases + messages (NEW) |
| X14 | Audit log | `/audit` | Existing audit package (extend) |
| X15 | Staff and roles | `/staff` | Admin users + roles (NEW) |
| X16 | Card programme | `/cards` | Waitlist now, issuer later |

---

## 5. ChatGPT instructions (admin console)

### 5.1 Master instruction (paste into a new ChatGPT chat)

```text
You are my tech lead for the NovaWallet ADMIN CONSOLE, a separate React + TypeScript + Vite web app for staff, backed by my Spring Boot API (novawallet-api).
You write prompts for opencode (an AI coding agent). You never write the code yourself.

Rules for every prompt:
1. One prompt = one small step (one screen, one shared component group, or one backend endpoint group).
2. Begin with GOAL, READ FIRST (admin/docs/ADMIN_GUIDE.md + the screen's BUILD.md + light.png/dark.png), SCOPE ("do not change anything outside these folders").
3. Stack is fixed: React 18, TypeScript strict, Vite, Tailwind, TanStack Query, TanStack Table, React Router, Recharts, Zod for API types, Vitest + Testing Library, Playwright for 1 end-to-end test per screen.
4. Admin auth uses httpOnly cookies set by the backend; never store tokens in localStorage.
5. Every action button checks the user's permission and calls the audit endpoint (or the backend audits it). Destructive actions use a confirm dialog with a required reason.
6. Maker-checker actions create a change request; they never apply directly.
7. Backend prompts target novawallet-api: use the existing admin and audit packages, Flyway migrations for new tables, @PreAuthorize with the new permissions, and integration tests. End with "./mvnw test passes; list changed files and migrations".
8. Frontend prompts end with "npm run lint && npm test pass; list changed files and anything unfinished".
9. After I paste opencode's summary, check it against the step's Done-when before giving the next prompt; write a fix-up prompt if needed.

Start by asking me to upload ADMIN_GUIDE.md and the README. Then give me prompt A0.1 only.
```

### 5.2 Step plan (ChatGPT turns each line into one opencode prompt)

**[B]** is a backend prompt and **[W]** is a web prompt. Do the steps in this order.

| Step | Type | Build | Done when |
|---|---|---|---|
| A0.1 | W | Create `novawallet-admin` (Vite React TS), Tailwind with the admin tokens (light/dark), fonts, ESLint/Prettier, env config (`VITE_API_BASE_URL`, `VITE_ENV=SANDBOX/LIVE`). | Blank app runs in both themes |
| A0.2 | W | Shared layout: Sidebar (groups, active state, count badges), TopBar (search ⌘K, env badge, theme toggle, notifications, avatar), Panel, KPI card, Status text (dot + label), Data table (sorting, pagination, empty/loading/error states), Filter chips, Confirm-with-reason dialog, Toast. | Storybook-style demo page matches X02/X03 shapes |
| A0.3 | W | API client (fetch + credentials: 'include'), Zod schemas for ApiResponse/ErrorResponse, TanStack Query setup, 401 → login redirect, permission hook `usePermission('kyc.approve')`. | Unit tests |
| A1.1 | B | Admin auth: `admin_users` table (separate from customers), roles SUPPORT/KYC_REVIEWER/FINANCE/SUPER_ADMIN, permissions, `POST /admin/auth/login` (password + TOTP), `POST /admin/auth/totp/setup`, httpOnly cookie JWT with admin audience, lockout, IP allowlist config. Replace `hasRole('ADMIN')` with permission checks on existing admin controllers. | Tests: wrong TOTP, lockout, a role without a permission gets 403 |
| A1.2 | W | X01 Staff sign in + TOTP setup flow. | Login works against dev |
| A2.1 | B | Metrics: `GET /admin/metrics/overview` (money in/out today, active users 7d, failed rate, 14-day series), `GET /admin/alerts`. | Tests with seed data |
| A2.2 | W | X02 Dashboard. | Matches design, refreshes every 60 s |
| A3.1 | B | Extend AdminUserController: search/filter/paginate, user detail aggregate (profile, security, limits, stats), audit "viewed user". | Tests |
| A3.2 | W | X03 Users, X04 User detail (freeze/unfreeze via existing AdminWalletController with reason). | Freeze shows in the app as frozen |
| A4.1 | B | KYC: reason codes, assign/lock endpoint, signed image URLs (5 min), second-reviewer rule for SUSPECTED_FRAUD, push to customer on decision. | Tests |
| A4.2 | W | X05 KYC queue, X06 KYC review. | Approve/reject updates the app (D08/D09) |
| A5.1 | B | Admin transactions: search endpoint, detail with ledger entries + event timeline, `POST /requery` (asks Lipila), `POST /resolve` creating a maker-checker change request. | Tests |
| A5.2 | W | X07 Transactions, X08 Transaction detail. | |
| A6.1 | B | Reconciliation views (needs backend Phase 1B Batch 3): summary, needs-action list, daily provider balances, "run now". | Tests |
| A6.2 | W | X09 Reconciliation. | |
| A7.1 | B | Provider routes table (provider, direction, enabled, message), `GET/PUT /admin/providers`, health stats per hour; app endpoints `/app/notices` and `/payment-methods` read it. | Turning Airtel collections off hides it in F01 and shows E05 |
| A7.2 | W | X10 Providers. | |
| A8.1 | B | Change requests (maker-checker) table + approve/reject endpoints; limits config tables; wire FeeController edits through change requests. | A change is not live until a 2nd admin approves |
| A8.2 | W | X11 Limits and fees (with pending-change banner and approve dialog). | |
| A9.1 | B | Notices CRUD + schedule + audience; push via FCM topic. | |
| A9.2 | W | X12 Notices with live phone preview. | |
| A10.1 | B | Support cases + messages (or connect a helpdesk), link to transactions. | |
| A10.2 | W | X13 Support cases. | |
| A11.1 | B | Audit log query endpoint (append-only table, DB user without UPDATE/DELETE). | |
| A11.2 | W | X14 Audit log, X15 Staff and roles (invite, role change). | |
| A12 | W+B | X16 Card programme (waitlist list + checklist; real card ops later). | |
| A13 | W | Final pass: keyboard navigation, focus states, dark mode check of every screen, Playwright smoke test of all routes. | All green |

### 5.3 Example prompt ChatGPT should produce

```text
GOAL: Build admin screen X10 "Providers" (kill switch) in novawallet-admin.
READ FIRST: admin/docs/ADMIN_GUIDE.md; admin/screens/X10_admin_providers/BUILD.md; light.png; dark.png.
SCOPE: src/features/providers/** and its tests only.
BUILD:
1. A card per provider from GET /api/v1/admin/providers: code badge, name, "via" text, success rate as status text, 24 health bars, two toggles (Collections, Payouts).
2. Turning a toggle OFF opens the confirm dialog with a required customer message (prefilled "… top ups are paused while … fixes a problem. Your balance is safe."). On confirm, PUT /api/v1/admin/providers/{code} {direction, enabled:false, message}.
3. Only users with permission providers.manage see enabled toggles; others see them disabled with a tooltip.
4. After success show a toast and refetch; on error show the server message.
TESTS: component test for permission states; Playwright: toggle Airtel collections off and see "Paused · notice shown to customers".
DONE WHEN: matches the PNGs in light and dark; the change appears in the audit log.
FINISH: npm run lint && npm test; list changed files and anything unfinished.
```
