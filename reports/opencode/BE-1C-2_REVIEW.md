# Review — BE-1C-2 (T1, independent; 2026-11-14)

Reviewer: separate agent, checked out `origin/feature/be-1c-2-authz` @ `7bc8288` in its own worktree. It did not write the change.

**Verdict: APPROVE WITH FIXES**

## Blockers
None.

## Majors (→ T2)
1. **Admin fee update bypasses bounds.** `PUT /api/v1/admin/fees/{id}` binds the `FeeConfiguration` entity (`@Valid @RequestBody FeeConfiguration`). The entity has no max on `percentageFee`, and `FeeEngineService` update copies fields with no `minFee <= maxFee` check. An admin can set a fee > 100%. Fix: `UpdateFeeRequest` DTO with the `CreateFeeRequest` constraints, plus a min ≤ max cross-check and a negative test. **→ fixed in T2.**
2. **Global `fail-on-unknown-properties: true`.** Provider payload classes are safe (`ignoreUnknown`). Client compatibility can't be checked because the app and admin have no code yet. **→ accepted risk:** recorded for APP-02/ADM-00; the clients must send exactly the contract fields.
3. **Traceability wording.** F2–F8 production code is in monorepo commit `6653046` (`git diff 6653046 HEAD -- novawallet-api/src/main` is empty). BE-1C-2 commits add tests and docs only. **→ report wording corrected in T2.**
4. **Weak assertions.** `InputBoundsIntegrationTest` asserts only status 400, so an unrelated 400 would pass. Fix: assert the error code / message path. **→ fixed in T2.**

## Minors (recorded, not fixed)
- The route-security allow-list is duplicated by hand. The test skips listed routes, so a permitAll removed from a listed route goes unnoticed (that change would be fail-safe).
- Payout account regex `^\+?\d{9,15}$` vs profile/register `^\+?[1-9]\d{9,14}$`: inconsistent. No MSISDN prefix check. No positive boundary tests.
- Fee-estimate `scale() > 2` rejects `100.000`, while `@Digits` DTOs accept trailing zeros.
- The 413 multipart path is not proven end-to-end (MockMvc).
- FeeController Javadoc wrongly says "No authentication required". ChangePassword max 100 vs 128.
- `handleTypeMismatch` echoes the raw parameter value (pre-existing, low).

## Reproduced evidence (reviewer)
- `./mvnw -B clean test` at `7bc8288`: **Tests run 366, Failures 0, Errors 0, Skipped 0**, BUILD SUCCESS.
- `git diff f8ee658..HEAD -- novawallet-api/src`: 4 files, +608/−1; the only changed existing line is the enum fix. No test deleted, skipped or weakened.

## Unverifiable
Client payload compatibility; end-to-end 413; provider payload field sets; Java 17 runtime (BLK-04).
