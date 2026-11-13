# APP-11 — Activity, detail, receipts, statements, report a problem (J01–J07)

**Area:** novawallet-app · **Needs:** APP-10 · **Screens:** J01, J02, J03, J04, J05, J06, J07

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): J01 · J02 · J03 · J04 · J05 · J06 · J07.

Endpoints: GET /wallets/{id}/transactions with filters/search/keyset pagination, GET /transactions/{ref}, receipt.pdf, statements, POST /support/cases.

Specifics:
- J01 grouped list with infinite scroll and pull-to-refresh; J02 filter sheet; J07 no results; J03 detail with status timeline and category change (PATCH category); J04 receipt view + PDF download/share; J05 statements by month; J06 report a problem creating a case with the transaction attached.
- Pending/unknown items auto-refresh with backoff (stop on leave); 500-row scroll performance check on a low-end emulator (frame stats in the report).

Integration: filters, search, receipt download, report a problem.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
