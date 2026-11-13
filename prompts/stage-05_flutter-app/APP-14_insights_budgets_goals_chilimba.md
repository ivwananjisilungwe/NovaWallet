# APP-14 — Insights, budgets, goals, round-ups, cash, Chilimba (M01–M09, Q06–Q09) 🔒

**Area:** novawallet-app · **Needs:** APP-13, BE-1D-10 done · **Screens:** M01, M02, M03, M04, M05, M06, M07, M08, M09, Q06, Q07, Q08, Q09

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): M01 · M02 · M03 · M04 · M05 · M06 · M07 · M08 · M09 · Q06 · Q07 · Q08 · Q09.

Endpoints: GET /insights/summary?month=, budgets, goals (+deposit/withdraw), round-ups, cash-entries, Chilimba endpoints.

Specifics:
- Charts exactly as drawn (use chart colours from tokens; draw with CustomPainter or a charting package themed to match; label values formatted as the design shows). M02/M03 budgets; M04–M06 goals; Q08/Q09 add/take money with PIN; M07 round-ups; M08 add cash; M09 + Q06/Q07 Chilimba creation and member invites.
- All money moves use the money-flow engine; amounts exact in ngwee.

Integration: create goal → deposit → withdraw; budget progress after a categorised spend; chilimba group creation.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
