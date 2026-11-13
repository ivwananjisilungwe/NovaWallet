# NovaWallet v5 · Part 10 · System diagrams

26 diagrams, each in light and dark, as PNG (2x) and SVG (editable, vector). Start with `00-overview-light.png`.

| # | Diagram | Type | What it shows | Used in |
|---|---|---|---|---|
| 01 | [Use case diagram](01-use-case/01-use-case-light.png) | Product and requirements | Actors and what each can do; «include» steps. | Requirements chapter, scope slide |\n| 02 | [System context (C4 level 1)](02-system-context/02-system-context-light.png) | Architecture | NovaWallet with users and outside systems. | Introduction to architecture |\n| 03 | [Target architecture (C4 level 2)](03-containers-target/03-containers-target-light.png) | Architecture | Services, event bus, providers and shared platform. | Architecture chapter, backend/01 |\n| 04 | [Evolution roadmap](04-roadmap-evolution/04-roadmap-evolution-light.png) | Architecture | Phases 0–9 from safe monolith to microservices. | Backend/02 completion plan |\n| 05 | [Deployment diagram](05-deployment-stages/05-deployment-stages-light.png) | Architecture | Pilot VM, growth, and scale stages. | Backend/08 deployment |\n| 06 | [Module map and dependency rules](06-module-map/06-module-map-light.png) | Architecture | Phase 4 modular monolith, sync calls vs events. | Backend/02 phase 4 |\n| 09 | [Activity: registration, PIN, KYC](09-activity-onboarding/09-activity-onboarding-light.png) | Activity | Swimlanes: Student, App, API, Compliance. | Screens A, B, C, E |\n| 10 | [Activity: login and session refresh](10-activity-login-session/10-activity-login-session-light.png) | Activity | Device binding, OTP, token rotation. | Screens C01–C06 |\n| 11 | [Activity: top up from mobile money](11-activity-topup/11-activity-topup-light.png) | Activity | Collection, webhook, re-query, ledger credit. | Screen F04, backend/03 |\n| 12 | [Activity: send money (P2P)](12-activity-send-money/12-activity-send-money-light.png) | Activity | Idempotency, PIN, one DB transaction. | Screens G, backend/02 |\n| 13 | [Activity: withdraw (three-stage payout)](13-activity-withdraw/13-activity-withdraw-light.png) | Activity | Hold, provider call, finalise, reconcile. | Screens H, backend/03 |\n| 14 | [Activity: virtual card issue, fund, freeze](14-activity-virtual-card/14-activity-virtual-card-light.png) | Activity | Token only, no card number stored. | Screens K, backend/04 |\n| 15 | [Activity: admin maker-checker](15-activity-maker-checker/15-activity-maker-checker-light.png) | Activity | Two-admin approval with audit. | Admin X07, X09 |\n| 16 | [Sequence: top up through Lipila](16-sequence-topup/16-sequence-topup-light.png) | Sequence | Messages between app, API, payments, Lipila, phone. | backend/03 |\n| 17 | [Sequence: wallet to wallet](17-sequence-transfer/17-sequence-transfer-light.png) | Sequence | Filters, locking, journal entry, outbox. | backend/02 |\n| 18 | [Sequence: payout in three stages](18-sequence-payout/18-sequence-payout-light.png) | Sequence | Stages A, B, C and reconciliation job. | backend/03 |\n| 19 | [Sequence: card authorisation](19-sequence-card-authorisation/19-sequence-card-authorisation-light.png) | Sequence | Issuer webhook, funding check, settlement. | backend/04 |\n| 20 | [Sequence: outbox and events](20-sequence-outbox-events/20-sequence-outbox-events-light.png) | Sequence | At-least-once delivery, idempotent consumer. | backend/01 stage 2 |\n| 21 | [State machines: payout and collection](21-state-payments/21-state-payments-light.png) | State | Allowed transitions and terminal states. | backend/03 |\n| 22 | [State machines: KYC, wallet, card](22-state-kyc-wallet-card/22-state-kyc-wallet-card-light.png) | State | Three gating state machines. | backend/04 |\n| 23 | [Entity relationship diagram](23-er-diagram/23-er-diagram-light.png) | Data | Core tables across identity, ledger, payments, cards. | backend/02 phase 2 |\n| 24 | [Double-entry ledger](24-ledger-entries/24-ledger-entries-light.png) | Data | Chart of accounts and four example entries. | backend/02 phase 2 |\n| 25 | [Security architecture](25-security-zones/25-security-zones-light.png) | Security | Trust zones and controls at each boundary. | backend/01 section 5 |\n| 26 | [CI/CD and blue-green release](26-cicd-blue-green/26-cicd-blue-green-light.png) | Operations | Pipeline from push to rollback. | backend/08 |

## How to use them

- **Report or dissertation:** use the PNG for Word or Google Docs. Use the SVG when you want crisp print or to edit text in Figma, Inkscape or draw.io (File → Import).
- **Presentations:** use the dark versions on dark slides, light on light.
- **Code reviews with opencode:** attach the matching diagram when you ask for a step, for example D12 and D17 for the transfer feature.

## Instructions for ChatGPT (paste as a message before using the diagrams)

```
You are helping me document and build NovaWallet, a Flutter + Spring Boot wallet app for Zambian students.
I will attach system diagrams (D01 to D26). Treat them as the source of truth for flows, states and tables.
Rules:
1. When I ask for a feature, name which diagrams apply (for example top up = D11, D16, D21, D24) and follow them step by step.
2. Never invent a state, table or endpoint that is not in the diagrams or in the backend docs. If something is missing, say so and ask me.
3. Money rules from the diagrams are mandatory: Idempotency-Key on money calls, PIN checked on the server, double-entry ledger, webhook re-query before crediting, three-stage payouts, no card numbers stored.
4. Write opencode prompts that are small, numbered, and end with a test the developer can run.
5. If a diagram and code disagree, list the differences and propose the smallest change to the code.
6. When I ask for a diagram change, give me a precise written change request (which box, arrow, label) instead of redrawing.
```

## Notes and honest gaps

- Table names, field names and amounts (K100, K2 fees) are design examples. Confirm real fees, limits and timeouts before building.
- D03, D05 and D06 show the target. Today the app is one Spring Boot service at Phase 1B Batch 2.
- Lipila behaviours (callback signing, field names) are marked VERIFY in backend/03 and still need confirming with Lipila.
- D19 shows the pre-funded card model. Use the just-in-time variant only if the issuing partner supports it.
