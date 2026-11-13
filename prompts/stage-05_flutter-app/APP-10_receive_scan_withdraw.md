# APP-10 — Receive, scan, pay a shop (H01–H03) and Withdraw (I01–I03) 🔒

**Area:** novawallet-app · **Needs:** APP-09 · **Screens:** H01, H02, H03, I01, I02, I03

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): H01 · H02 · H03 · I01 · I02 · I03.

Endpoints: GET /qr/me, QR resolve, POST /merchant-payments, GET /payment-methods, withdraw/payout endpoints, status polling.

Specifics:
- H01 my QR (brightness boost, share/save), H02 scanner (camera permission states, torch, invalid/expired QR messages), H03 pay a shop review → PIN → result.
- I01 withdraw (limits and fee quote), I02 review, I03 processing with status polling and unknown-outcome handling; wallet balance reflects the hold immediately and the final result after.

Integration: scan valid and forged QR, withdraw happy path and unknown outcome.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
