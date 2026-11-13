# APP-07 — Home, notifications, service notices (E01–E07)

**Area:** novawallet-app · **Needs:** APP-06 · **Screens:** E01, E02, E03, E04, E05, E06, E07

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): E01 · E02 · E03 · E04 · E05 · E06 · E07.

Endpoints: GET /wallets/me, GET /wallets/{id}/transactions?size=5, GET /kyc/status, GET /app/notices, GET /notifications, /unread-count, PUT /notifications/{id}/read, PUT /notifications/read-all.

Specifics:
- App shell with the bottom tab bar exactly as drawn (labels, icons, active pill); tab state preserved.
- E02 hero balance card, quick actions, recent transactions; E01 ID-in-review banner; E03 balance hidden (persisted toggle, masked as drawn); E04 new user empty state; E05 service notice banner (from notices, dismissible per notice id).
- E06/E07 notifications list with category tabs, unread styling, tap → deep link target; empty state.
- Skeleton loading P07/P08 here too (shared components), cache-first rendering with stale marker.

Integration: seeded users covering E01, E02, E04 render the right variants; hide balance persists across restart.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
