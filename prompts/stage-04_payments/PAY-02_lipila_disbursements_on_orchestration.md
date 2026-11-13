# PAY-02 — Move Lipila payouts/withdrawals onto the orchestration engine 🔒

**Area:** novawallet-api · **Needs:** PAY-01 · **Screens:** G07, I01–I03, P03

---
Follow `_COMMON_FEATURE_RULES.md`.

Goal: the existing three-stage payout becomes a `DISBURSEMENT` payment intent handled by a `LipilaProvider` adapter — same safety, same public API, no regression.

Do:
1. Wrap the current Lipila gateway code as `LipilaProvider` implementing the SPI (disbursement, status query, disbursement webhook parsing). Keep its sandbox configuration. Do not change request/response mappings unless the provider docs in `docs/providers/lipila/` say so.
2. `POST /wallets/{walletId}/payouts` and withdraw (I01–I03) create intents and run through the engine: stage A (hold + ledger) → stage B (provider call outside any DB transaction) → stage C (finalise). `PENDING_RECONCILIATION` maps to the engine's UNKNOWN state.
3. Migrate in-flight payouts: a one-off, idempotent migration that creates intents for existing non-terminal payouts; dry-run report first.
4. Retire the old payout-specific scheduler/webhook paths only after equivalence tests pass (run old vs new behaviour tests side by side).
5. Response fields for the app: status, human status message code, `nextCheckAfter`, reference — enough for I03 "processing" and P03 "status unknown".
6. Withdraw vs "send to mobile money" (G07): confirm from the BUILD.md whether they differ only in recipient (own linked number vs any number) and model accordingly.

Tests: every Phase 1B payout test still passes unchanged; simulator scenarios; crash between stages; duplicate/late webhook; reconciliation resolving UNKNOWN both ways; ledger balanced after each.
