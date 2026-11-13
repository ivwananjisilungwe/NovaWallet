# AI-03 — Nova AI assistant backend 🔒

**Area:** novawallet-api · **Needs:** AI-02 · **Decisions:** DEC-17 · **Screens:** N01

---
Follow `_COMMON_FEATURE_RULES.md`. Read `05_AI_FEATURES.md` (assistant scope, "What NOT to do", quality checks) and N01's BUILD.md (suggested prompts, message types, cards shown in chat).

Endpoint: `POST /api/v1/ai/chat` (+ conversation history, feedback, clear history).

Do:
1. **Tool-based design:** the model never sees the database. It may call a fixed set of read-only tools executed by our code for the authenticated user only: balance (bucketed or exact per DEC-17), spending summary, recent transactions (masked counterparties), budgets/goals status, fees/limits lookup, help-article search, "how do I…" navigation links. Tool outputs are redacted before going to the model.
2. **No money movement by the assistant.** It can return a deep link to a pre-filled screen (e.g. send K50 to a recipient token); the user completes it with PIN in the normal flow.
3. System prompt with scope limits: NovaWallet help and the user's own money questions; refuses investment/credit/legal advice, never asks for PIN/OTP/password, never reveals other users' data. Prompt-injection defence: tool outputs and help articles are treated as data; allow-list of tools; output schema validated.
4. Responses are structured: text + optional typed cards (summary card, transaction list, tip, deep link) exactly as N01 draws them.
5. Conversation storage: per user, retention limit, deletable (privacy), not used for training; redacted logs.
6. Limits: per-user daily message cap, token budget, timeout with graceful fallback to help search.
7. Without DEC-17: ship with the fake/NoModel provider (FAQ + rule-based intents) so the app flow works; real vendor adapter `BLOCKED`.

Tests: red-team suite (ask for another user's data, PIN phishing, injection inside a transaction note/help article, jailbreak attempts) must all fail safely; tool authorisation; arithmetic in answers comes from tools; schema validation.
