# NovaWallet — AI Strategy Roadmap

> **Last verified:** 2026-08-22 against the current codebase (`novawallet-api` service inventory, Flyway V1–V11 schema, `docs/SRS.md`, `docs/PROJECT-PLAN.md`) and `HANDOFF_2026-08-22.md`.
>
> **Companion doc:** [`COMPETITIVE-ANALYSIS.md`](COMPETITIVE-ANALYSIS.md) — the market rationale for phases A and B below.
>
> **Status framing:** `docs/SRS.md` deliberately lists AI features as post-MVP ("over-engineering for MVP"). This roadmap keeps that discipline: nothing here ships before the wallet core and Phase 11 rails are stable.

---

## 1. Principles

1. **Build on what exists.** Every phase below anchors on tables and services already in the codebase — no parallel data store, no rip-and-replace.
2. **Rules before models.** Deterministic rules ship first; ML only once labeled outcomes accumulate.
3. **Advisory by default.** AI output informs users and admins; it never moves money or restricts accounts autonomously.
4. **Per-user data boundaries.** The ownership-check pattern already enforced in `TransactionHistoryService` applies to every new surface.

## 2. Architecture anchors (available today)

| Anchor | Where | Why it matters for AI |
|---|---|---|
| Transaction ledger | `Transaction` table via `TransactionRepository`; debit/credit pairs + linked FEE records | Primary training/feature data: type, status, amount, timestamps, balanceBefore/After |
| Append-only audit log | `AuditLog` via `@Audited` AOP (`AuditedAspect`, async writes) | Behavioral event stream: logins, PIN events, admin actions |
| Velocity signals | `TransactionLimitService` + `TransactionVelocityConfig` (10 tx/h, 50,000 ZMW/h) | Existing anomaly thresholds — seeds for a risk engine |
| Idempotency records | `IdempotencyKey` / `IdempotencyService` | Duplicate-attempt patterns = replay-abuse signal |
| Auth friction signals | `LoginRateLimiter` (email+IP lockout), PIN lockout (3 fails → 15 min) | Account-takeover features |
| Notification pipeline | `NotificationService` (+ `MailService`, `SmsService` stub), retry job | Delivery channel for insights and alerts |
| KYC pipeline | `KycService`, `AdminKycService`, `FileStorageService`, `KycDocument` | Document store for OCR pre-fill; manual review is the human gate |
| Admin console | `AdminService` + admin controllers | Human-in-the-loop surface for any model-flagged case |

---

## 3. Data readiness at a glance

| Phase | Usable TODAY | Needs building | Gap severity |
|---|---|---|---|
| A — Insights | Full per-user ledger history; fee records; notification delivery | Nullable `category` column + user-correction capture | Low — rules work starts immediately |
| B — Risk | Velocity counters, audit events, idempotency replays, lockout signals | Labeled fraud outcomes; risk-rule config store | Medium — rules now, labels accrue over months |
| C — Assist | Ownership-checked history queries; stored KYC documents | FAQ corpus (authored); OCR capability | Medium-high — content and tooling both new |
| D — Credit/forecasting | Ledger-derived cash-flow features (post-launch volume) | Months of real volume; consent framework; outcome labels | High — groundwork only until rails go live |

### 3.1 Effort & skills breakdown

| Phase | Backend work | Frontend work | Data/ML work | Net size |
|---|---|---|---|---|
| A — Insights | `insights` package + `category` migration (small) | Digest widgets in `features/wallet` (small) | Rules only, no ML | **S–M** |
| B — Risk | `risk` package, config store, admin flag queue (medium) | Flag-review surface in admin console (small) | Rule tuning now; ML is a separate later project | **M → L** |
| C — Assist | Support module + retrieval over owned history (medium) | Chat surface + OCR confirmation flow (medium) | FAQ authoring + extraction tooling choice (medium) | **M / L** |
| D — Credit/forecasting | Scheduled aggregate jobs reusing cron pattern (medium) | Clearly-labeled projection views (small) | Feature definitions + consent framework (large) | **L** |

---

## 4. Phased roadmap

### Phase A — Quick wins: spending categorization & smart notifications · **Effort: S–M**

- **Goal:** Auto-categorize transactions and deliver useful summaries instead of raw lists.
- **Data available today:** Full ledger history per user (type/status/amount/description/timestamps); fee records; notification delivery with retries.
- **Data needed:** None new to start — categorization begins **rules-based** on `TransactionType` + description patterns (e.g., `[LIPILA:<referenceId>]` deposit markers, fee-linked records). A nullable `category` column (Flyway V12+) enables user corrections that later seed a classifier.
- **Integration points:** New `insights` package reading through `TransactionRepository` / `TransactionHistoryService`; aggregation jobs following the existing scheduler pattern (`PendingTransactionCleanupJob`, `BalanceRecalculationJob`); delivery via `NotificationService` with new `NotificationType` values; Flutter surfaces in `features/wallet` using Riverpod providers.
- **Privacy/compliance:** Aggregates are computed per-user from the user's own data only. Insights are informational — no advice framing that could drift into regulated territory under Bank of Zambia (BoZ) supervision.
- **Acceptance criteria:** ≥80% of transactions auto-categorized by rules at launch; weekly digest opt-out respected; categorization adds <50 ms to history reads (cache-friendly).

### Phase B — Risk: fraud & anomaly detection on the transaction stream · **Effort: M (rules) → L (ML)**

- **Goal:** Catch compromised accounts and abuse patterns before losses compound.
- **Data available today:** Velocity counters (`TransactionLimitService`), append-only `AuditLog` events, idempotency replay attempts (`IdempotencyService`), PIN lockouts, login lockouts (`LoginRateLimiter`). This is a genuine feature set *today*.
- **Data needed:** Labeled outcomes for ML later — admin freeze/unfreeze decisions (already audited with reasons) plus confirmed-fraud markers become weak labels over time.
- **Integration points:** Rules engine evaluated where velocity checks already run inside `TransactionService` (pre-PIN enforcement point); new `risk` package; scheduled scoring job alongside existing cron pattern; flags surfaced in the admin console (`AdminService`) rather than auto-actioned.
- **Privacy/compliance:** **No autonomous account restriction** — flags route to human review; freeze/unfreeze stays an admin action with reason + audit trail, exactly as built. Rule hits support BoZ-aligned AML/CFT monitoring but do not replace statutory reporting obligations. False-positive handling needs a user-facing appeal path before launch.
- **Acceptance criteria:** Rules ship behind config (`app.risk.*`, mirroring `app.transaction.velocity`); every flag lands in the admin queue with evidence links into `AuditLog`; zero silent account actions in code review.

### Phase C — Assist: conversational support & KYC OCR pre-fill · **Effort: M (assistant MVP) / L (production OCR)**

- **Goal:** Self-service answers grounded in the user's own history; faster KYC submissions.
- **Data available today:** Per-user transaction history (ownership-checked queries exist); KYC documents stored via `FileStorageService`. **Missing:** an FAQ corpus (must be authored) and any OCR capability.
- **Integration points:** New support module exposing retrieval over the user's own `TransactionHistoryService` results plus curated FAQs; KYC pre-fill hooks beside `KycService` upload flow, with extracted fields shown to the user for confirmation before submission; admin review remains in `AdminKycService`.
- **Privacy/compliance:** KYC documents are sensitive PII — **no transmission to third-party model APIs without legal review and a data-processing agreement**; prefer on-device or self-hosted extraction first. Assistant responses must never cross the per-user ownership boundary. Retention policy for OCR intermediates required before launch.
- **Acceptance criteria:** Assistant answers cite the underlying transaction/reference or FAQ source; refusal behavior when asked about other users' data; OCR output never auto-submits — user confirms every pre-filled field.

### Phase D — Advanced: credit-readiness groundwork & cash-flow forecasting · **Effort: L**

- **Goal:** Prepare the data spine for future credit products and predictive budgeting — without shipping either prematurely.
- **Data available today:** Ledger-derived cash-flow features (inflow/outflow regularity, balance volatility, fee burden) computable from `TransactionRepository` aggregates once live volume exists post-rails.
- **Data needed:** Months of real transaction volume (impossible pre-launch); repayment outcomes if lending ever proceeds; consent framework for any secondary use of financial data.
- **Integration points:** Analytics layer over existing repositories; forecasting runs on the user's own history only; results rendered in `features/wallet` as projections, clearly labeled as estimates.
- **Privacy/compliance:** Credit scoring intersects lending regulation — this phase produces **readiness groundwork only**. No scoring product, no lender partnerships, no decisions until licensing posture is resolved with BoZ requirements.
- **Acceptance criteria:** Feature definitions documented but not computed in production until volume thresholds are met; any forecasting UI carries explicit "estimate, not advice" labeling.

### Phase quality gates (consolidated)

| Phase | Gate | Pass condition |
|---|---|---|
| A | Categorization coverage | ≥80% of transactions auto-categorized by rules; user corrections persisted |
| B | Safety invariants | Zero silent account actions in code review; every flag links to `AuditLog` evidence; appeal path live |
| C | Grounding & boundaries | Every answer cites a transaction reference or FAQ source; ownership-boundary tests green |
| D | Restraint | No production computation before volume threshold; estimate-only labeling verified |

---

## 5. What we will NOT do

- ❌ **No autonomous money movement.** Models never initiate deposits, transfers, or payouts; every debit requires explicit user action + PIN, and payout initiation stays behind `PayoutController`'s PIN/velocity/email-gate checks.
- ❌ **No automated account freezes or limits from AI output.** Flags go to humans; admin actions stay audited.
- ❌ **No credit or lending decisions without licenses.** Phase D is groundwork, not a lending product.
- ❌ **No raw KYC documents or personal data sent to external model providers** without legal review and a DPA.
- ❌ **No training on cross-user data without consent and anonymization** — per-user boundaries hold even for model inputs.
- ❌ **No AI features ahead of rails stability.** Phase 11 (payments) and FCM push land first; sequencing follows `PROJECT-PLAN.md`.

---

## 6. Risk register

| Risk | Mitigation |
|---|---|
| Rules engine false positives block legitimate users | Human-review gate + appeal path; thresholds start loose and tighten with label data |
| Model/API dependency leaks PII | DPA-first procurement; self-hosted options preferred for anything touching KYC docs |
| Insights misread as financial advice | Fixed informational copy; no recommendation language; legal copy review before launch |
| Effort creep on Phase B/C "L" items | Ship the rules/retrieval MVP first; ML and production OCR are separately gated decisions |

---

## 7. Sequencing summary

```
Phase 11 rails stable ──► Phase A (S–M) ──► Phase B rules (M) ──► Phase C assistant (M)
                                                    │                        │
                                              labels accrue            OCR hardening (L)
                                                    ▼                        ▼
                                            Phase B ML (L)          Phase D groundwork (L)
```

*Success measure for every phase: it changes a user's or admin's decision using data NovaWallet already lawfully holds — nothing more.*
