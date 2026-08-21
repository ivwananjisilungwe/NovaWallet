# Phase C — Offline Layer (design doc, follow-up implementation)

This doc captures the offline architecture so the full Hive-backed
implementation can be completed in the next milestone without re-discovering
the plan. A lightweight `OfflineBanner` widget already ships in this PR.

## Decisions
- Cache engine: `hive_flutter` (typed boxes for wallet, transactions, profile)
- Connectivity: `connectivity_plus` stream → top banner
- Pattern: stale-while-revalidate providers — emit cached value on start,
  refresh in background, keep last-good on error.
- Outbox: Hive box `pending_ops` with `Idempotency-Key` so flush is safe.

## Deferred (next PR)
- `flutter pub add hive_flutter connectivity_plus`
- `lib/core/cache/hive_boxes.dart` — box open at bootstrap
- Provider wrappers that read cache first
- Pending-ops flush on connectivity restore
- Golden tests for OfflineBanner

The banner already in `lib/core/widgets/offline_banner.dart` is wired to
provider error states (no new dep required) and unblocks the ship gate.
