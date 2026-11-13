# APP-09 — Send money, mobile-money send, request, split (G01–G09) 🔒

**Area:** novawallet-app · **Needs:** APP-08, BE-1D-09 done · **Screens:** G01, G02, G03, G04, G05, G06, G07, G08, G09

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): G01 · G02 · G03 · G04 · G05 · G06 · G07 · G08 · G09.

Endpoints: GET /users/lookup, POST /contacts/match (via Q13), GET /fees/estimate, POST /transfers, GET /payouts/name-check, POST /wallets/{id}/payouts, POST /payment-requests, POST /splits.

Specifics:
- Money-flow engine from AGENTS.md §6 shared by G02–G06 and G07 (amount → recipient → review → PIN → result).
- G01 recipient choice (recents, search, lookup, contacts), G02 amount keypad, G03 review (amount, fee, total, recipient name), G04 PIN sheet, G05 wrong PIN with attempts remaining, G06 success with share/receipt actions.
- G07 send to mobile money: network detect, name-check result or NAME_CHECK_UNAVAILABLE copy, review, PIN, result including processing/unknown outcome (P03).
- G08 request money; G09 split a bill with exact ngwee shares and the remainder rule from the backend.
- One Idempotency-Key per attempt reused on retry; double-tap protection; leaving mid-flight after PIN shows the in-progress state.

Integration: transfer success, wrong PIN, insufficient funds, payout unknown outcome, split creation.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
