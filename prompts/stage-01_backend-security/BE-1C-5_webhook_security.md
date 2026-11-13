# BE-1C-5 — Phase 1C Batch 5: Webhook & external integration security 🔒 ⛔

**Area:** novawallet-api · **Needs:** PAY-02 · **Blocked by:** BLK-01 / DEC-07 (Lipila collections signing contract)

---
Follow `_COMMON_BATCH_RULES.md` for Batch 5.

**Pre-check:** open `docs/providers/lipila/` and `docs/DECISIONS.md`. If the Lipila collections signing contract (header name, algorithm, encoding, canonical string, timestamp header, tolerance, event-id field) is not present as a provider document, STOP immediately and report `BLOCKED: BLK-01`. Do not invent a scheme. The 503 guard on the collections webhook stays.

If the contract is present, scope (plan §7 Batch 5): SEC-05, NEW-OPS-01.

Do:
1. Implement signature verification exactly per the provider doc: constant-time comparison, timestamp tolerance, raw-body verification (before JSON parsing), key from env/secret store, key rotation support (two active keys).
2. Replay protection: event-id dedup in `provider_webhook_events` (reuse the disbursement design), reject stale timestamps.
3. Apply the same review to the disbursement webhook (shared-secret today?) — upgrade to the documented signature if the provider supports it.
4. Optional IP allow-list via config (documented as defence in depth, never the only check).
5. Webhook handlers are idempotent and never trust amounts/status without matching an existing intent (reference + amount + currency + wallet). Mismatch → quarantine record + alert, no balance change.
6. Remove the 503 guard only when all tests pass; record evidence.

Tests: valid signature, bad signature, missing headers, stale timestamp, replay, mismatched amount, unknown reference, rotated key.
