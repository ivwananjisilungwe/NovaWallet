# PAY-03 — Top-up: mobile-money collections 🔒 ⛔

**Area:** novawallet-api · **Needs:** BE-1C-5 (or its BLOCKED fallback below) · **Blocked by:** BLK-01 for the real Lipila adapter · **Screens:** F01–F08

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/v5/part-08/03_MOBILE_MONEY_INTEGRATION.md` §3 and every F-screen BUILD.md.

Endpoint: `POST /api/v1/wallets/{walletId}/collections` (+ status by reference). The disabled `/deposit` endpoint stays disabled.

Do:
1. Flow: choose method/linked number (F01) → amount with limits (F02/F03) → review with fee (F04, `GET /fees/estimate?type=DEPOSIT`) → create COLLECTION intent → provider sends the USSD/PIN prompt to the customer's phone (F05 "Approve on your phone") → webhook/status → wallet credited **only on confirmed success** (F06) / failed (F07) / pending beyond the wait window (F08).
2. Ledger: provider clearing ↔ wallet liability posted only on confirmation; no provisional credit.
3. Status endpoint supports polling with `nextCheckAfter`; intent expires after the provider's timeout window; late success after expiry is still credited exactly once (webhook dedup + idempotent transition).
4. Limits: KYC level, per-transaction, daily/monthly, balance cap (config from BE-1D-04; engine from PAY-04 when available).
5. **Provider:** build and test against the sandbox simulator now. Implement the real `LipilaProvider.initiateCollection` + webhook verification **only if** the signed contract is in `docs/providers/lipila/` and BE-1C-5 is done; otherwise leave the real adapter `BLOCKED: BLK-01`, keep the collections webhook 503 guard in production profile, and gate the feature with the `topup` flag (off in production).
6. Fraud basics: collections only from numbers linked/verified to the user unless the owner decides otherwise (DEC).

Tests: full simulator matrix (approve, decline, timeout, late approve, duplicate webhook, amount mismatch → quarantine), limits, idempotent create, ledger assertions, feature flag off → `FEATURE_DISABLED`.
