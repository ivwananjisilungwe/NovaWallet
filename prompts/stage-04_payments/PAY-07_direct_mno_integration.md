# PAY-07 — Direct MTN / Airtel integration (later, optional) ⛔

**Area:** novawallet-api · **Needs:** PAY-02, PAY-03 live for some months · **Trigger:** volume makes aggregator fees worth replacing, and the owner has signed with the operator

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/v5/part-08/03_MOBILE_MONEY_INTEGRATION.md` §1 and §9.

**Pre-check:** the operator's official API documentation and sandbox credentials (outside the repo) must exist in `docs/providers/<operator>/`. If not, STOP: `BLOCKED: operator contract`. Do not build from memory of public APIs.

Do:
1. New adapter implementing the existing `PaymentProvider` SPI for the operator: collection (request-to-pay), disbursement, status query, name lookup, callback verification, token management (OAuth/API keys with refresh and secure storage), error-code mapping to our stable codes.
2. Routing rules: by network prefix → operator direct, with Lipila as fallback; per-operation kill switches (already in PAY-01); shadow mode first (query-only) before real traffic.
3. Separate clearing accounts in the ledger per provider; reconciliation importer for the operator's statement format.
4. Certification checklist from the operator (UAT cases) turned into automated tests against their sandbox where allowed.
5. Rollout: 0 % → internal numbers only → 5 % → 50 % → 100 % by config, with automatic fallback on error-rate threshold.

Tests: SPI contract test suite (the same suite every provider must pass), routing/fallback, token refresh, callback verification.
