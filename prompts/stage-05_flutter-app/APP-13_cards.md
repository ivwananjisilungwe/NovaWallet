# APP-13 — Cards (L01–L07) 🔒

**Area:** novawallet-app · **Needs:** APP-12, PAY-06 done · **Screens:** L01, L02, L03, L04, L05, L06, L07

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): L01 · L02 · L03 · L04 · L05 · L06 · L07.

Endpoints: POST /cards/waitlist (and, only when the card partner module exists, /cards, /cards/{id}/controls, /freeze, /fund, /transactions).

Specifics:
- L01 waitlist is the launch state. L02–L07 are built from the designs but stay behind the `cards` flag in "issuing" mode; with no partner they must remain unreachable in production builds.
- L04 card details come from the partner secure reveal — the app never stores or logs PAN/CVV; FLAG_SECURE and auto-hide after a timeout.
- If PAY-06 Part B is BLOCKED: build L01 fully and L02–L07 against the simulator, flagged off, and say so.

Integration: waitlist join; (simulator) create, freeze, fund.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
