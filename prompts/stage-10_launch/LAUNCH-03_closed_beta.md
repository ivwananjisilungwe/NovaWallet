# LAUNCH-03 — Closed beta with Mulungushi students

**Area:** product ops · **Needs:** APP-23, LAUNCH-02

---
1. Invitation mechanism: invite codes/allow-list (admin tool or config), cap 50 → 200 → 500 users, per-wave goals.
2. Onboarding material: one-page how-to, WhatsApp/support channel (DEC-23), in-app help articles reviewed, beta terms (counsel).
3. **Feedback loop:** in-app "report a problem" cases (J06/O11), weekly review template, crash reports, analytics events agreed (no PII/amounts), triage labels, SLA for money issues (same day).
4. **Success metrics & halt criteria** (write numbers with the owner): sign-up → KYC completion rate, first top-up rate, 7-day retention, payment success rate ≥ 99 %, unknown-outcome rate, support tickets per 100 users, crash-free sessions ≥ 99.5 %, any ledger drift = stop.
5. Daily routine: proof check, reconciliation queue, stuck intents, support queue; weekly: backup verification result, dependency alerts.
6. Fix loop: T3 prompts for bugs, T2 for review findings; hotfix procedure via OPS-03 (staged Play rollout, backend first).
7. Exit criteria to move to production rollout written and signed by the owner.
