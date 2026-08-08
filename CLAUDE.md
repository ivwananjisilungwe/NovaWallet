# NovaWallet — CLAUDE.md

Claude Code alias for the NovaWallet project instructions.

This project uses an **orchestrator + subagents** setup. Read the canonical instructions in `AGENTS.md` (opencode convention) and follow them in full — they apply to Claude Code as well:

- Detect the stack: `novawallet-api/` = backend (Spring Boot), `novawallet-app/` = mobile (Flutter).
- Mobile tasks load **frontend + backend** skills; backend tasks load **backend** skills.
- Dispatch to the matching specialist subagent (`mobile`, `backend`) per subfolder; run both in parallel for fullstack work.
- Verify: backend `./mvnw test`, mobile `flutter analyze && flutter test`.