# NovaWallet — Phase 1B Final File Change Inventory

**Document:** PHASE_1B_FINAL_FILE_CHANGE_INVENTORY.md
**Date:** 2026-11-12 (final remediation cycle)
**Branch:** feature/phase-1b-safe-payout-processing
**Baseline commit:** aac8737e6c435f1e3c09d6756b001f5363ff9f61
**HEAD:** aac8737e6c435f1e3c09d6756b001f5363ff9f61 (uncommitted, unstaged)
**Git state verified:** 18 modified tracked files + 25 untracked files + 0 staged (2026-11-12).

## A. Implementation (source + configuration) — 18 files

Tracked modified (13 main source/config + 5 docs counted under D):

1. `src/main/java/.../notification/schedule/BalanceRecalculationJob.java`
2. `src/main/java/.../payment/controller/PayoutController.java`
3. `src/main/java/.../payment/controller/WebhookController.java`
4. `src/main/java/.../payment/service/PayoutService.java`
5. `src/main/java/.../payment/service/WebhookService.java`
6. `src/main/java/.../transaction/entity/Transaction.java`
7. `src/main/java/.../transaction/enums/TransactionStatus.java`
8. `src/main/java/.../transaction/repository/TransactionRepository.java`
9. `src/main/java/.../transaction/schedule/PendingTransactionCleanupJob.java`
10. `src/main/java/.../user/repository/UserRepository.java`
11. `src/main/resources/application-dev.yml`
12. `src/main/resources/application-prod.yml`

Untracked (new):

13. `src/main/java/.../payment/config/SystemClockConfig.java`
14. `src/main/java/.../payment/entity/ProviderWebhookEvent.java`
15. `src/main/java/.../payment/entity/ProviderWebhookEventStatus.java`
16. `src/main/java/.../payment/repository/ProviderWebhookEventRepository.java`
17. `src/main/java/.../payment/schedule/PayoutReconciliationScheduler.java`
18. `src/main/java/.../payment/service/PayoutReconciliationService.java`

## B. Migrations — 3 files

19. `src/main/resources/db/migration/V13__alter_transaction_status_length.sql`
20. `src/main/resources/db/migration/V14__add_payout_data_columns.sql`
21. `src/main/resources/db/migration/V15__provider_webhook_events_and_reconciliation_indexes.sql`

## C. Tests — 17 files

22. `src/test/java/.../endpoint/DisbursementWebhookControllerTest.java`
23. `src/test/java/.../endpoint/PayoutEnabledIntegrationTest.java`
24. `src/test/java/.../endpoint/PayoutIdempotencyHttpTest.java`
25. `src/test/java/.../notification/schedule/BalanceRecalculationJobDisabledInProdTest.java`
26. `src/test/java/.../payment/service/DisbursementWebhookServiceTest.java`
27. `src/test/java/.../payment/service/PayoutCompensatingCreditTest.java`
28. `src/test/java/.../payment/service/PayoutConcurrentIdempotencyTest.java`
29. `src/test/java/.../payment/service/PayoutErrorPropagationTest.java`
30. `src/test/java/.../payment/service/PayoutFinalizationIdempotencyTest.java`
31. `src/test/java/.../payment/service/PayoutIdempotencyTest.java`
32. `src/test/java/.../payment/service/PayoutReconciliationBackoffTest.java`
33. `src/test/java/.../payment/service/PayoutReconciliationServiceTest.java`
34. `src/test/java/.../payment/service/PayoutStageCTransactionBoundaryTest.java`
35. `src/test/java/.../payment/service/PayoutTimeoutAndReconciliationTest.java`
36. `src/test/java/.../transaction/schedule/PendingTransactionCleanupJobTest.java`
37. `src/test/resources/application-test.yml`
38. `src/test/resources/logback-test.xml`

## D. Documentation — 10 files

In-repo (5):

39. `docs/architecture/backend-risk-register.md`
40. `docs/architecture/current-architecture.md`
41. `docs/architecture/domain-boundaries.md`
42. `docs/architecture/implementation-roadmap.md`
43. `docs/architecture/payout-lifecycle.md`

Parent-level (outside the git repo, in `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet/reports/`):

44. `reports/PHASE_1B_FINAL_SECURITY_REVIEW.md`
45. `reports/PHASE_1B_FINAL_ACCEPTANCE_REPORT.md`
46. `reports/PHASE_1B_FINAL_REMAINING_RISKS.md`
47. `reports/PHASE_1B_FINAL_FILE_CHANGE_INVENTORY.md`
48. `reports/PHASE_1B_FINAL_VERIFICATION_LOG.md`

## E. Excluded (must NOT enter the Phase 1B commit)

- `novawallet-api/report/` (gitignored) — 10 historical Phase 0/1A/1B reports, preserved as historical record.
- `novawallet-api/target/`, `novawallet-api/uploads/`, `novawallet-api/logs/`, `.idea/`, `.serena/` — ignored build/runtime artifacts.
- `novawallet-api/TODO.md` — removed (unauthorized duplicate; canonical TODO is the parent-level `TODO.md`).
- `novawallet-api/reports/` — removed (unauthorized subagent artifacts with fabricated claims).
- No secrets, no generated build artifacts, no staged changes.

## Reconciliation

- In-repo files that would enter the commit: 18 (A) + 3 (B) + 17 (C) + 5 (D in-repo) = 43.
- Modified tracked: 18. Untracked: 25. Staged: 0. Sum: 43 in-repo = 18 modified + 25 untracked. ✓
- Every file appears exactly once. No duplicates. No missing files. No unrelated work.

**No commit performed. No push performed. No reset/rebase performed.**