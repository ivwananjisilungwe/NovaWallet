# NovaWallet v5 · Part 08 · Architecture, backend completion, payments, cards, AI, compliance

Read in this order:

| # | Document | What it gives you |
|---|---|---|
| 01 | `01_SYSTEM_ARCHITECTURE.md` | Today's monolith → modular monolith → events → microservices, module ownership, infrastructure and security design (with diagrams) |
| 02 | `02_BACKEND_COMPLETION_PLAN.md` | Every remaining backend task from Phase 1B Batch 3 onward, mapped to your risk-register IDs and to the app screens, plus a 12-week order |
| 03 | `03_MOBILE_MONEY_INTEGRATION.md` | MTN, Airtel and Zamtel through Lipila: top-up and withdrawal flows, webhook safety, reconciliation, error messages, test and go-live checklists, direct-telco options |
| 04 | `04_VIRTUAL_CARDS.md` | How card issuing works, partner leads to verify, funding models, PCI rules, card module design, launch steps |
| 05 | `05_AI_FEATURES.md` | 16 AI features in three phases, the safe architecture (read-only tools, consent, no money movement), quality checks |
| 06 | `06_COMPLIANCE_ZAMBIA.md` | Licence vs partner model, laws to check, how the design supports compliance, pilot checklist (not legal advice) |
| 07 | `07_CHATGPT_INSTRUCTIONS_BACKEND.md` | Master prompt + 40-step plan for ChatGPT to write opencode prompts for all of the above |

Based on the repo scan of `NovaWallet.zip` (Phase 0, 1A, 1B Batch 2 done; Batch 3 not started) and research on Lipila, MTN, Airtel, card issuers and BoZ rules. Items marked **VERIFY** must be checked with the provider or regulator before going live.

Also save these docs in your repo as `docs/v5/` so opencode can read them.
