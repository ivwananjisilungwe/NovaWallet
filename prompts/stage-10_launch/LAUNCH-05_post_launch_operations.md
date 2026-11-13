# LAUNCH-05 — Post-launch operations (recurring)

**Area:** operations · **Needs:** LAUNCH-04

---
Cadence (put dates in the owner's calendar):
- **Daily:** daily proof result, reconciliation exceptions, unknown/pending intents older than SLA, support queue, alerts.
- **Weekly:** backup verification result, dependency/security updates (Dependabot PRs through the normal gates), error budget review, fee/provider cost review.
- **Monthly:** restore drill, access review (staff, roles, keys), secret rotation check, pen-test finding follow-up, limits/fees review (DEC-15 changes via maker-checker), compliance reporting inputs for counsel.
- **Quarterly:** disaster-recovery game day, load test refresh, roadmap review: AI-05 forecasting, PAY-07 direct MNO, PAY-06 cards, ARC-03 extraction gate (only with measured triggers), new features each as a plan-first prompt (T5) with a design in the same style as the v5 parts.
Each recurring item has a checklist in `infra/runbooks/`. Anything new follows the same pipeline: design (screens), backend prompt, app prompt, admin prompt, review, gates.
