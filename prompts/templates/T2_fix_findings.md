# T2 — Fix review findings (new session)

Paste after a T1 review. Replace placeholders.

---
Read root `AGENTS.md` and `<AREA>/AGENTS.md`. The change for `<PROMPT-ID>` is uncommitted in the working tree. An independent review found these issues:

<PASTE THE BLOCKERS AND MAJORS FROM THE REVIEW>

For each finding:
1. Confirm it is real by reading the code (if you disagree, explain with evidence and do not change that part).
2. Fix it minimally, in scope. Add or strengthen a test that would have caught it.
3. Do not change unrelated code. Do not weaken any test or safeguard.

Then run the full gates for the area and update `reports/<PROMPT-ID>_REPORT.md` with a "Review fixes" section: finding → fix → test → evidence. Stop and report.
