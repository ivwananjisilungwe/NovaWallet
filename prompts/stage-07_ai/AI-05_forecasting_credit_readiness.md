# AI-05 — Cash-flow forecasting & credit-readiness groundwork (post-launch, optional)

**Area:** novawallet-api · **Needs:** at least 3 months of real pilot data · **Trigger:** owner decision after launch

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/AI-STRATEGY.md` Phase D and §5 "What we will NOT do".

Do:
1. **Forecast:** per-user upcoming-balance projection from recurring patterns (allowances, bills, airtime habits) using transparent statistical methods first (seasonal averages, recurring-payment detection); output ranges, not single numbers; shown as insight cards with a reason. Back-test on historical data; report error.
2. **"Low balance ahead" and "bill due soon" nudges** via notifications with preferences respected.
3. **Credit-readiness groundwork (data only):** consistent feature definitions (income regularity, balance volatility, on-time bill patterns), consented data collection flag, documentation. **No credit scores, no lending decisions, no sharing with third parties** — lending needs its own licence review (counsel) and a separate project.
4. Fairness and privacy review document: features excluded on purpose, opt-out, retention.
5. Admin read-only analytics for aggregate trends (no individual profiling screens).

Tests: forecast back-test harness, nudge rate limits, opt-out honoured.
