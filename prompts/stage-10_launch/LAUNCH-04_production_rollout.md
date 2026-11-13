# LAUNCH-04 — Production rollout 🔒

**Area:** release · **Needs:** LAUNCH-03 exit criteria met, pen-test findings (critical/high) closed, legal checklist items done

---
1. Final gate review: T4 owner checklist across api/app/admin/infra; OPS-07/08 reports; fidelity report; backup restore drill within the last 30 days; runbooks current; support staffed.
2. Release order: backend (expand migrations) → admin → app. Play staged rollout 5 % → 20 % → 50 % → 100 %, each step held at least 48 h while metrics stay within the halt criteria; force-update only for critical fixes.
3. Launch-week command centre: owner/dev on watch, alert routing tested, status messaging prepared (X12 notices + support macros).
4. Marketing hooks are out of scope for the agent; list what the product needs ready (store listing, invite flow, referral rules approved).
5. Post-launch checkpoint at day 1, 3, 7, 30: metrics vs targets, incidents, support themes, backlog re-prioritisation → feeds `LAUNCH-05`.
