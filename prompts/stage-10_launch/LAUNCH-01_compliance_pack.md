# LAUNCH-01 — Compliance pack (assemble facts; counsel decides) ⛔

**Area:** docs · **Needs:** OPS-09 · **Blocked by:** DEC-20 (licence route), DEC-21, DEC-22, counsel

---
Read `docs/v5/part-08/06_COMPLIANCE_ZAMBIA.md` (notes, not legal advice), `docs/DECISIONS.md`, `docs/architecture/`.

You are not a lawyer; you do not decide what the law requires. Produce factual documents for counsel and the licensed partner:
1. **System description for regulators/partners** `docs/compliance/SYSTEM_DESCRIPTION.md`: what the product does, flows of funds (use the ledger design and diagrams), where money is held, who the providers are, data collected and stored, security controls actually implemented (cite files/tests), limits and fees (from config), KYC levels, complaints handling, incident response (runbooks).
2. **Data inventory** (every field, purpose, retention, location, third parties) → input for the Data Protection Act position and privacy policy; list processors (SMS, push, hosting, AI vendor if any).
3. **AML/CFT inputs:** transaction monitoring rules in force (AI-02), screening gaps (sanctions/PEP screening not implemented unless added), record-keeping capabilities, export formats; list questions for counsel on thresholds and reporting.
4. **Consumer protection inputs:** fee disclosure screens, error resolution process, dispute SLAs, terms drafts needed (DEC-22) — placeholders for counsel's text.
5. **Open questions list** for counsel, ordered by launch impact (licence route, partner agreement, safeguarding of customer funds, under-18 users, Chilimba/group savings treatment, referral rewards, cards).
6. Checklist `LAUNCH_READINESS_LEGAL.md` with each item `OWNER + COUNSEL TO COMPLETE`.
Do not state or imply the product is licensed, compliant or approved.
