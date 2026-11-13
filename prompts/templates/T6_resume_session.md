# T6 — Resume an interrupted or confused session (fresh session)

---
Read root `AGENTS.md` and `<AREA>/AGENTS.md`. You are continuing `<PROMPT-ID>` (prompt file: `prompts/<path>.md` — read it).

A previous session did part of the work and stopped (context full / error / confusion). Do NOT redo finished work blindly:
1. Inspect `git status --short` and `git diff --stat` to see what exists. Read `reports/<PROMPT-ID>_REPORT.md` or `_PLAN.md` if present.
2. Run the gates to see the current state (which tests fail, analyzer issues).
3. Write a short status: done / partly done / not started, per item in the prompt's Do list.
4. Continue from the first unfinished item. Keep going until the prompt's acceptance criteria are met, then report.

If the previous work is wrong or unsafe, say so and propose: fix in place, or ask the owner to discard it (`git restore` is for the owner, not you).
