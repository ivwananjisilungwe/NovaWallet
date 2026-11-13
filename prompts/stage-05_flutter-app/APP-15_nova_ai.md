# APP-15 — Nova AI (N01–N02)

**Area:** novawallet-app · **Needs:** APP-14, AI-03 done · **Screens:** N01, N02

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): N01 · N02.

Endpoints: POST /ai/chat, GET /ai/tips, tip feedback, clear history.

Specifics:
- N01 chat with typed cards (summary, transactions, tip, deep link) exactly as drawn, suggested prompts, typing/loading and failure states, history, clear history; the assistant never moves money — deep links open the normal flow.
- N02 tips list with dismiss/feedback; feature flag `ai`; graceful fallback when the AI is unavailable (help search).

Integration: ask a balance question (tool-backed), tap a deep link into G02 pre-filled.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
