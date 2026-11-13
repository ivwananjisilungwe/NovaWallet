# AI-01 — AI gateway, smarter categorisation, Nova tips

**Area:** novawallet-api · **Needs:** BE-1D-12 · **Decisions:** DEC-17 (LLM vendor + data policy) · **Screens:** N02, M01

---
Follow `prompts/stage-02_backend-app-api/_COMMON_FEATURE_RULES.md`. Read `docs/AI-STRATEGY.md` and `docs/v5/part-08/05_AI_FEATURES.md` (feature list, "What NOT to do", gateway architecture, endpoints). Build Phase A only.

Do:
1. **AI gateway module** (the only place that may call a model): provider interface, request/response logging **without PII**, a redaction layer (names, phone numbers, account/ID numbers, exact balances replaced or bucketed before any external call), per-user and global budgets, timeouts, caching, kill switch + feature flag `ai`, deterministic fallback when the model is unavailable.
2. If DEC-17 is not decided: implement a local rule-based `NoModelProvider` and a fake provider for tests; the real vendor adapter is `BLOCKED: DEC-17`. No API keys in the repo.
3. **Categorisation assist:** for transactions the rules cannot classify, ask the gateway with merchant/biller text only (no user identity); store confidence; user overrides always win; evaluation set of ≥ 200 labelled examples with accuracy reported.
4. **`GET /api/v1/ai/tips`** (N02): tips generated from the user's own aggregates (BE-1D-11 insights): template-first (rules pick the tip, numbers injected by code — the model never invents figures); optional model rewording through the gateway. Each tip: id, text, reason, action deep link, dismiss/feedback endpoint.
5. Smart alerts: unusual spend vs. own baseline, budget nearing limit, bill due patterns — as notifications (respecting preferences).
6. Guardrails doc `docs/ai/GUARDRAILS.md`: no financial advice claims, no credit decisions, no model access to raw PII, human-readable reason for every tip.

Tests: redaction tests (PII never leaves), budget/kill switch, fallback path, tip arithmetic equals insights data, evaluation report.
