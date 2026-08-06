# Session Handoff — NovaWallet UI Design in Stitch

**Date:** 2026-08-05
**Context:** NovaWallet (Zambian digital wallet — Spring Boot backend + planned Flutter mobile app)
**Stitch project:** `projects/11542754846512650236` (title: **NovaWallet**, PRIVATE, PROJECT_DESIGN)
**Design system asset:** `assets/13836462354037157009` ("NovaWallet Design System", v1)

---

## What This Session Did

1. **Connected to the Stitch MCP server** via the `stitch-mcp` stdio bridge (`.planning/mcp-connect.mjs`) and the API key in `~/.cline/data/settings/cline_mcp_settings.json`.
2. **Created the NovaWallet Stitch project** (`projects/11542754846512650236`).
3. **Scanned the entire Spring Boot backend** (`novawallet-api`, ~150 Java files, 17 modules) to ground the UI in the real domain/API.
4. **Created a professional mobile-first design system** in Stitch.
5. **Generated 30 UI screens** (26 prompt-driven screens; onboarding/success produced multiple variants) — all mapped 1:1 to real backend endpoints.
6. **Built reusable tooling** in `.planning/` to keep generating/editing Stitch screens.

---

## Backend Scan Summary (what the UI maps to)

**Endpoints surfaced from controllers** — all under `/api/v1`:
- **Auth:** `POST /auth/register` (firstName, lastName, email, phone `+260…`, password), `POST /auth/login`, `POST /auth/refresh`
- **Email:** `POST /email/verify?token=`
- **Password:** `POST /password/forgot`, `POST /password/reset` (token + new password)
- **PIN:** `POST /pin` (4–6 digit, `^\d{4,6}$`)
- **Users:** `GET /users/me`, `PUT /users/me` (firstName, lastName, phone)
- **Wallets:** `GET /wallets/me` (balance, accountNumber `NW…`, status ACTIVE/FROZEN, freezeReason, currency ZMW)
- **Transactions (auth + `Idempotency-Key` header):**
  - `POST /wallets/{id}/deposit` (amount, description)
  - `POST /wallets/{id}/withdraw` (amount, pin, description)
  - `POST /wallets/{id}/transfer` (receiverWalletId, amount, pin, description)
  - `GET /wallets/{id}/transactions` (type/status/from/to/page/size), `GET /transactions/{reference}`, `GET /wallets/{id}/balance`
- **Fees:** `GET /fees/estimate?type=&amount=` → percentageFee, flatFee, minFee, maxFee, totalFee
- **KYC:** `POST /kyc/documents/upload` (documentType + file ≤10MB jpeg/png/webp/pdf), `GET /kyc/status`, `POST /kyc/submit`
- **Admin (ADMIN role):** users list/deactivate, `GET /admin/kyc/pending`, KYC detail/approve/reject, wallet freeze/unfreeze, transactions search, audit logs, fee CRUD under `/admin/fees`

**Key domain facts reflected in the UI:**
- Currency is **ZMW** (Zambian Kwacha); phone prefix **+260**; references `NWTXyyyyMMdd…`
- Transaction types: DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT, FEE; status: PENDING / SUCCESSFUL / FAILED
- Wallet status: ACTIVE / FROZEN (freeze reasons: SUSPICIOUS_ACTIVITY, ADMIN_ACTION, USER_REQUEST)
- **KYC tiers (application.yml):**
  - Tier 1 **Basic** — wallet ZMW 5,000 / daily send ZMW 2,000 — requires `NATIONAL_ID`
  - Tier 2 **Standard** — ZMW 50,000 / 20,000 — `NATIONAL_ID` + `SELFIE`
  - Tier 3 **Advanced** — ZMW 200,000 / 100,000 — `NATIONAL_ID` + `SELFIE` + `PROOF_OF_ADDRESS`
- **Wallets are created only after KYC approval** (not at registration) — confirmed project decision.
- Password rule: 8+ chars with upper/lower/digit/special; login rate-limited (5 fails / 15 min); PIN lockout 3 attempts / 15 min.

> Full onboarding/UX narrative is in `USER-FLOW.md` (4-tab nav: Home / Send / Cards / Profile). Note: virtual cards & mock mobile-money deposit methods are **V2/post-MVP** in the current backend.

---

## Design System (created in Stitch)

`assets/13836462354037157009` — "NovaWallet Design System" v1
- **Color mode:** LIGHT · **Roundness:** ROUND_FULL
- **Headline font:** GEIST · **Body/label font:** INTER
- **Primary:** `#4F46E5` (indigo) · **Secondary:** `#10B981` (emerald — money-in/success)
- **Tertiary:** `#F59E0B` (amber — pending/KYC) · **Neutral:** `#0F172A`

---

## Screens Generated (26 → 30 screen instances)
All under project `projects/11542754846512650236/`:

| Slug | Stitch screen |
|------|------|
| 01-register | `screens/6501dead94264d9cae3e04b1153f2ab0` |
| 01-onboarding | `screens/dd7837068b964552bedb0277d686374b`, `screens/48eb651f24c44f129402614303b8c59d`, `screens/69d96917ef00402c9939be85b7e53369` |
| 02-login | `screens/7d90d23fad6b4706b1ecb5e9b6b567ce` |
| 03-forgot-password | `screens/7d8dac65778b4eb6a27c222bab1a74cd` |
| 04-reset-password | `screens/0517ae7d8c2648be842652840bbaebd6` |
| 05-email-verify | `screens/70eff07557b14472991c5823fd0f8b97` |
| 06-set-pin | `screens/5c85d5e4796541a4a927d48e564f83ea` |
| 07-home-dashboard | `screens/e4826006782a46488465c78c08cb3c50` |
| 08-transaction-history | `screens/deccb94af0694c85a4ca7f702f24da26` |
| 09-transaction-detail | `screens/d07133f35f3c4bbe8e52ed99c22e7f00` |
| 10-deposit | `screens/40acab9bafaa4112aaa9361308d49f86` |
| 11-withdraw | `screens/a06b0fca36a14b95a694782554946555` |
| 12-send-transfer | `screens/2010684831f342e7a28db06fcc609d1d` |
| 13-transfer-confirm-pin | `screens/89101c2f61e14b23b52d6009c53d9411` |
| 14-fee-estimate | `screens/523ca4991edb4e8b849ac775f5ec5b89` |
| 15-success | `screens/69573e803e3848e48d984ef2d9192a18`, `screens/c03b3e8f035141bf946b0f2fff684f48` |
| 16-kyc-status | `screens/a5e052a73cdb4cf78a5c09773a089353` |
| 17-kyc-upload | `screens/dc2df7301d43457aa993e4cb014203de` |
| 18-profile-settings | `screens/c3d2cad654a0488299d56ea318b5ca56` |
| 19-edit-profile | `screens/5160d7476cd042e69639b5010a644d62` |
| 20-virtual-cards | `screens/7ff7db12083e41dbb847774af0295a67` |
| 21-admin-dashboard | `screens/b7d750ec00044dafaa1487a0dad6933b` |
| 22-admin-kyc-review | `screens/b5549efdb14a4f3f9d25ebcd5a547ece` |
| 23-admin-users | `screens/c0f752277d4144a0bc5cea2d4d27ae1f` |
| 24-admin-transactions | `screens/ac5841b2920443dc8057abd620ed46bc` |
| 25-admin-fees | `screens/ecea6792b5f1476b9b3ee00679e36f4f` |

**Result: 26/26 prompts succeeded → 30 screen instances.** One transient failure (`11-withdraw`) was retried successfully.

---

## Tooling Added in `.planning/` (reusable)

- **`mcp-connect.mjs`** — general bridge to call any Stitch MCP tool: `node .planning/mcp-connect.mjs stitch <tool> '<jsonParams>'`
- **`stitch-gen.mjs`** — generate a screen from a prompt file: `node .planning/stitch-gen.mjs <slug> <promptFile>` (writes `/tmp/stitch_<slug>.json`)
- **`stitch-batch.mjs`** — sequential batch generation over all `.planning/prompts/*.txt`; writes/reads `/tmp/stitch_manifest.json`
- **`stitch-manifest.json`** — persisted screen↔project manifest for this session
- **`prompts/*.txt`** — 26 detailed, backend-grounded screen prompts (numbered 01–25; `01-register` + `01-onboarding`)

**Run a screen generation in the background (Stitch takes ~30–45s):**
```bash
cd /media/.../NovaWallet
setsid bash -c 'node .planning/stitch-gen.mjs myscreen .planning/prompts/01-onboarding.txt > /tmp/out.log 2>&1' </dev/null >/dev/null 2>&1 &
# poll: cat /tmp/stitch_myscreen.json
```

**MCP config location:** `~/.cline/data/settings/cline_mcp_settings.json` (server name `stitch`).

---

## Known Notes / Limitations

- **`create_project` and `generate_screen_from_text` reject unknown fields** — pass exactly the schema fields. Generation works reliably with just `projectId` + `prompt`; the project's design system is auto-applied (passing the `designSystem` field threw "invalid argument", so it was omitted).
- Some prompts produced multiple screen instances (onboarding → 3, success → 2). These are variants in the project.
- Screen previews/logs live in `/tmp/stitch_*.json` and `stitch_manifest.json` (persisted copy in `.planning/`).
- Virtual cards (20) and mobile-money deposit methods (10) reflect **V2/post-MVP** features from `USER-FLOW.md` that the current backend does not yet expose.

---

## Suggested Next Steps

1. **Review the screens** in the Stitch UI; `apply_design_system` or refine any that look off.
2. **Add missing screens** if desired (card creation form, QR scan, buy airtime, notifications list, statements).
3. **Generate the Flutter implementation** from these screens for the mobile app (the remaining post-MVP piece).
4. Optionally create a **DARK** variant of the design system and generate a few dark screens.
5. Keep this manifest up to date as you iterate; reuse `.planning/stitch-gen.mjs` for new screens.

