# APP-17 — Offline, errors, system states (P01–P09) and extra flows (Q01–Q03, Q05, Q10–Q13) 🔒

**Area:** novawallet-app · **Needs:** APP-16 · **Screens:** P01, P02, P03, P04, P05, P06, P07, P08, P09, Q01, Q02, Q03, Q05, Q10, Q11, Q12, Q13

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): P01 · P02 · P03 · P04 · P05 · P06 · P07 · P08 · P09 · Q01 · Q02 · Q03 · Q05 · Q10 · Q11 · Q12 · Q13.

Endpoints: PIN reset endpoints (BE-1D-03), incoming request pay/decline, change phone, verify email, legal documents, contacts match.

Specifics:
- Wire the system screens to the real triggers: P01 no connection, P02 home offline (cached data + banner), P03 payment status unknown (from unknown/pending intents), P04 server error with request id, P05 maintenance, P06 update required (Play link), P07/P08 skeletons everywhere, P09 back-online toast.
- Q01–Q03 reset PIN (code → selfie → new PIN); Q05 incoming money request; Q10 change phone number (OTP both numbers); Q11 verify email; Q12 terms and privacy with version acceptance; Q13 find friends with contacts permission rationale and hashed matching (no contact list stored).

Integration: offline cache behaviour, maintenance toggle, PIN reset path, incoming request pay.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
