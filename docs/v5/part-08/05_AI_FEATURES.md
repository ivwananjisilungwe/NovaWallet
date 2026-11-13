# 05 · AI features for NovaWallet

**The rule for every feature:** AI can *read and explain* the customer's own data, and *suggest* actions. It can **never move money or change settings by itself**. Any action opens the normal screen, where the customer confirms with their PIN.

There is a slot for the AI features document you said you would upload. When you send it, I'll merge it into this plan.

---

## 1. Feature list (prioritised)

### Phase A: launch with the pilot (cheap, high value)

| # | Feature | What the user gets | How it works | Screens |
|---|---|---|---|---|
| A1 | **Auto-categorisation** | Every payment gets a category (Food, Transport, Airtime…) without typing | Rules first (merchant table, bill type, keywords), then a small model for unknown merchants; learns from the user's corrections (J03 "change category") | J01, J03, M01 |
| A2 | **Monthly money summary** | "You spent K 1,240 in November, 12% less than October. Food was your biggest category." | Numbers computed in SQL; an LLM only words them nicely | M01, push on the 1st |
| A3 | **Nova tips** | 1–3 practical tips with one action each | Rule engine finds patterns (budget at 80%, bill due, goal ahead/behind, repeated small spends); LLM writes the text | N02, M01 tip card |
| A4 | **Ask Nova (chat)** | Questions in plain English: "How much did I spend on food?", "Can I afford K 300 for the trip?" | LLM with **read-only tools** that call your own summary endpoints (section 3) | N01 |
| A5 | **Bill and budget reminders** | "TV bill due Saturday", "Food budget 80% used" | Scheduled jobs + notifications (no LLM needed) | E06 |

### Phase B: after the pilot

| # | Feature | Notes |
|---|---|---|
| B1 | **Smart budget suggestions** | Suggest budget limits from 3 months of history (M03 info banner). |
| B2 | **Goal planner** | "Save K 200/week to reach K 8,000 by March": maths in code, wording by LLM (M06). |
| B3 | **Natural-language actions** | "Send 50 to Bwalya for lunch" → opens G03 Review prefilled; the user still confirms with PIN. |
| B4 | **Receipt / cash scanning** | Photo of a receipt → amount and category prefilled in M08 (on-device OCR such as Google ML Kit, so images stay on the phone). |
| B5 | **Local languages** | Nova replies in Bemba or Nyanja (test quality with native speakers before launch). |
| B6 | **Voice input** | Speech-to-text into the chat box. |

### Phase C: safety and operations (high value, mostly not LLM)

| # | Feature | Notes |
|---|---|---|
| C1 | **Fraud and risk scoring** | Score every transfer/payout using velocity, new device, new recipient, unusual amount, time of day and SIM change if available. High score → extra PIN/selfie, delay, or block + admin review. Start with rules, then add a gradient-boosted model when you have data. |
| C2 | **Scam warning** | Before sending to a new number a large amount, show "Is someone pressuring you to send this?" (G03). Never block silently. |
| C3 | **KYC assist** | Document quality check, OCR of NRC fields, face match and liveness through a KYC vendor (e.g. Smile ID; VERIFY Zambia support). Humans make the final decision on edge cases (X06). |
| C4 | **Support copilot** | Summarise a case and suggest a reply for the agent (X13). The agent edits and sends; the AI never replies directly to customers about money problems. |
| C5 | **Reconciliation explainer** | For admins: "Why is this payout stuck?", answered from the timeline (X08). |

---

## 2. What NOT to do

- No AI that approves loans or credit (that is a regulated product, and bias risk is high).
- No sending of full names, phone numbers, NRC numbers, card data or exact balances to the LLM unless a feature truly needs them (most don't).
- No AI-written messages that say money "arrived" or "failed": those always come from the ledger state.
- No training of third-party models on customer data. Use providers that offer zero data retention for API use (VERIFY each provider's terms).

---

## 3. Architecture (AI gateway module)

```mermaid
flowchart LR
  App[N01 chat] -->|POST /ai/chat| GW[ai gateway module]
  GW --> C{consent on?<br/>O12 toggle}
  C -->|no| R1[reply: turn on in Privacy]
  C -->|yes| T[Tool router]
  T --> S1[getMonthlySummary]
  T --> S2[getCategorySpend]
  T --> S3[getBudgets / getGoals]
  T --> S4[getUpcomingBills]
  S1 & S2 & S3 & S4 --> DB[(read-only views)]
  T --> LLM[LLM API]
  LLM --> F[Output filter<br/>no PII, no promises,<br/>actions → deep links only]
  F --> App
```

- **Tools are read-only functions you write** (e.g. `getCategorySpend(month)`). They return small JSON (category totals, not raw transactions). The LLM never gets database access.
- **Deep links instead of actions:** the model can return `{"action":"open","route":"/budgets/new?category=food&limit=600"}`. The app shows a button; the user taps it and confirms in the normal screen.
- **Prompt injection:** merchant names and notes are user-controlled text. Pass them as data fields, tell the model they are untrusted, and never follow instructions found in them.
- **Limits:** cap each user at about 30 messages a day and about 1,000 tokens per answer. Cache monthly summaries.
- **Logging:** store metadata (time, tools used, tokens, latency). Don't store full prompts unless the user opts in to "Help improve Nova".
- **Model choice:** any mainstream LLM API with tool/function calling works (e.g. Anthropic Claude, OpenAI, Google Gemini). Keep it behind an interface so you can switch. Check the data-processing terms and where data is processed (doc 06).
- **Cost control:** use a small/fast model for categorisation and tips, and a stronger model only for chat. Batch the monthly summaries overnight.

## 4. Quality checks before launch

- A test set of 100 real-style questions with expected answers (checked against SQL). Target: 95%+ numerically correct, with 0% wrong numbers stated confidently. When unsure, Nova says so.
- Red-team prompts: "send all my money to…", "ignore your rules", a merchant name containing instructions, a request for someone else's balance. All must be refused or safely handled.
- Accessibility: answers are short, in plain language, and readable by TalkBack.

## 5. Endpoints

| Endpoint | Purpose |
|---|---|
| `POST /ai/chat` | Streaming answer (SSE) for N01; body `{message, conversationId?}` |
| `GET /ai/tips` | Tips for N02 / M01 |
| `POST /ai/tips/{id}/dismiss` | "Not now" |
| `POST /ai/categorise` (internal) | Category for a new transaction |
| `GET /ai/summary?month=` (internal, cached) | Monthly summary JSON |
| Admin: `POST /admin/ai/case-summary/{caseId}` | Support copilot (C4) |
