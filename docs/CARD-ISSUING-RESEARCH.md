# NovaWallet — Card Issuing Research Track

> **Status:** Active research · **Last verified:** 2026-08-22
> **Scope:** Giving every NovaWallet user their own virtual Visa/Mastercard, funded from their ZMW wallet balance, spendable at Google/Netflix/Steam/international merchants.
> **Domain rule:** Card issuing is a SEPARATE concern from payment collection. Backend will expose a `CardIssuingProvider` interface (`create/freeze/unfreeze/fund/setLimit/close/getTransactions`) alongside — never inside — `PaymentGateway`.

---

## 1. Verified lead: Mastercard × Scale 🥇

**Source:** Mastercard press release, 31 Mar 2026 (+ TechCabal 26 Mar, TechAfrica News 30 Mar). Builds on their Aug 2024 partnership.

| Fact | Detail |
|---|---|
| Markets | Senegal, Côte d'Ivoire, Kenya, **Zambia**, Zimbabwe |
| Model | Single integration: onboarding + processing + **BIN sponsorship** + operations + compliance |
| Partners | Mastercard (network, banks, expertise) + Scale (Johannesburg issuing-infrastructure startup, founded 2022, CEO Miranda Naidoo) |
| Stated Zambia-market use cases | Companion **wallet cards**, SME cards, corporate spending, payout cards — i.e., exactly NovaWallet's shape |
| Products | Virtual **and** physical programmes |

**Why this leads:** it collapses the historically slow BIN-sponsor + issuing-bank + processor assembly into one partner that already covers Zambia, backed by the network itself. ChitChat proved Zambian consumer appetite for wallet cards via Mastercard (Sept 2024 launch).

## 2. Candidate shortlist

| Rank | Provider | Thesis | Key risk/question |
|---|---|---|---|
| 🥇 | **Scale + Mastercard** | Regulated regional issuer-orchestrator, Zambia named, single integration | Operational depth in ZM unproven ("next 12 months will be telling" — TechCabal); pricing unpublished |
| 🥈 | **Codego** | Visa+MC issuing API: virtual/physical, KYC/KYB, limits, PIN mgmt, HMAC webhooks | Zambia eligibility? Who is BIN sponsor? Funding model ZMW→USD? |
| 🔶 | **Fyatu** | Cheapest self-serve ($5–10/card, stablecoin funding) | Program control, compliance posture for reselling to ZM residents |
| 🔶 | **Kulipa** | Visa/MC APIs | Stablecoin-funded model mismatches ZMW-first product |
| 🔶 | **Gpaynow / BSICARDS** | Issuing APIs advertised | Marketing-heavy sites; treat as unverified until diligence passes |

## 3. Diligence question bank (ask EVERY provider)

1. Can you onboard a Zambian fintech/startup and issue to Zambian residents?
2. Who is the BIN sponsor / issuing bank, and in what country?
3. Visa, Mastercard, or both?
4. Can cards be funded from a **ZMW** balance (who does FX, at what spread)?
5. USD denomination available for international spend?
6. Per-card issuance fee, monthly/platform minimums, FX fee, authorization fee, chargeback fee?
7. Real-time authorization/transaction webhooks? HMAC signing?
8. Freeze/unfreeze latency? Per-user spending limits? MCC blocks?
9. 3-D Secure support? Decline handling flow (esp. Google/Netflix/Steam acceptance — test BINs before commit)?
10. Chargeback/dispute process and liability?
11. KYC obligations falling on NovaWallet vs provider? BoZ regulatory posture required?
12. Sandbox quality vs production parity? Production SLA?

## 4. Competitive context

- **MTN already ships a MoMo Virtual Mastercard** in Zambia for international payments. NovaWallet's differentiation cannot be "we also have a card" — it must be: multi-operator funding (MTN+Airtel+Zamtel) + spending controls + transparent fees + full transaction ledger + budgeting/AI insights (docs/AI-STRATEGY.md).
- **ChitChat** validated demand (Mastercard virtual debit, Sep 2024) but carries its own UX/pricing complaints — undercut opportunity documented in COMPETITIVE-ANALYSIS.md.

## 5. Sequencing decision (agreed)

1. **Phase 11** — Lipila rails live (prerequisite for funding anything).
2. **Phase 12** — Digital Goods storefront on ONE pooled card (~$5): fastest revenue-proving path to "online payments," serves teens day one without per-user issuance cost or KYB delay.
3. **Phase 13 — NovaWallet Card Platform** (per-user issuing via Scale/Mastercard): begins when (a) Phase 11/12 revenue exists, (b) Scale KYB approved, (c) unit economics modeled from real pricing. Digital-goods then becomes one consumer of personal cards.
