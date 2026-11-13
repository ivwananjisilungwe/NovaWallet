# LAUNCH-02 — Provider go-live (live keys, tiny real transactions) 🔒 ⛔

**Area:** infra, config · **Needs:** LAUNCH-01 progress, Lipila live onboarding done, BE-1C-5 completed with real contract · **The agent never receives live keys.**

---
Prepare, the owner executes:
1. `docs/launch/PROVIDER_GOLIVE.md`: step list from `03_MOBILE_MONEY_INTEGRATION.md` §8 adapted to what exists: live credentials injected through OPS-06 secrets; webhook URLs registered; IP allow-lists; signing keys rotated from sandbox values; `prod` profile checks (simulator impossible, placeholder fees refused).
2. **Go-live test plan** with tiny real amounts from owner-controlled accounts: top-up each network, payout each network, failed number, timeout handling, webhook delivery, reconciliation file match, fee correctness, settlement to the bank account, daily proof — each step with expected ledger entries and the stop condition.
3. Hard limits during the pilot enforced in config: very low per-transaction/daily limits, balance caps, small user allow-list (feature flag by user id), kill switches tested.
4. Monitoring during go-live: dashboards and alerts from OPS-05 checked; who watches, for how long.
5. Rollback: disable live provider via kill switch, refund procedure for any stuck customer funds.
6. Evidence form for each test (screenshots/log lines/reference ids) saved to `docs/launch/golive-evidence/` without secrets.
