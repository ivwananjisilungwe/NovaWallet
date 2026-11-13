# T5 — Plan first (use before any large or unclear step)

Prefix any prompt with this when the step is big, or the agent struggled last time.

---
Before writing any code for the task below, produce a plan only:
1. What you read (files, docs) and the relevant facts you found (with paths).
2. Exact files you will create/change, and why each.
3. Data/migration changes (with migration names) and the expand → migrate → contract plan.
4. API changes (method, path, request, response, errors).
5. Risks and how each is tested.
6. Decisions needed (DEC ids) with your recommendation.
7. Steps in order, each small enough to verify.

Write the plan to `reports/<PROMPT-ID>_PLAN.md` and stop. Do not modify source files. I will reply "approved" or with changes.

TASK:
<PASTE THE PROMPT HERE>
