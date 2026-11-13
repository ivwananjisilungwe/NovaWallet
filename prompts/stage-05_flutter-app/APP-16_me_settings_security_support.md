# APP-16 — Me, settings, security, devices, support, privacy, close account (O01–O13) 🔒

**Area:** novawallet-app · **Needs:** APP-15 · **Screens:** O01, O02, O03, O04, O05, O06, O07, O08, O09, O10, O11, O12, O13

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): O01 · O02 · O03 · O04 · O05 · O06 · O07 · O08 · O09 · O10 · O11 · O12 · O13.

Endpoints: GET/PUT /users/me, GET /security/overview, POST /security/lock, devices, POST /pin/change, payment methods, notification preferences, referrals, help articles, support cases, POST /privacy/export, POST /users/me/close.

Specifics:
- O01 profile hub; O02 personal details; O03 security centre (lock account action); O04 devices list/remove; O05 change PIN; O06 linked mobile money (OTP to verify a number); O07 notification settings; O08 appearance (theme/language persisted server-side too); O09 invite friends (referral code, share sheet); O10 help centre (articles, search); O11 support chat; O12 privacy and data (export request); O13 close account (preconditions from the API, PIN, confirmation).
- Logout wipes tokens and local cache; Terms/Privacy (Q12) reachable.

Integration: change PIN, remove a device, create a support case and reply, request export.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
