# APP-08 — Top-up (F01–F08) 🔒

**Area:** novawallet-app · **Needs:** APP-07, PAY-03 done in backend · **Screens:** F01, F02, F03, F04, F05, F06, F07, F08

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): F01 · F02 · F03 · F04 · F05 · F06 · F07 · F08.

Endpoints: GET /payment-methods, GET /fees/estimate?type=DEPOSIT, POST /wallets/{id}/collections (+status polling).

Specifics:
- F01 method/number choice (linked numbers from O06 data); F02 amount with live limits; F03 over-limit variant using the server error code and remaining allowance; F04 review with quoted fee/total (quote id).
- F05 "Approve on your phone" waiting screen with the countdown and polling by `nextCheckAfter`; F06 success; F07 failed with the mapped reason; F08 pending beyond the window (never shown as failed; link to Activity detail).
- Late success after leaving the app: next launch refreshes pending intents and shows a notification/toast; wallet balance updates.
- Feature flag `topup` off → entry hidden; sandbox magic amounts exercise every state.

Integration: success, decline, timeout-pending, late success, over-limit.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
