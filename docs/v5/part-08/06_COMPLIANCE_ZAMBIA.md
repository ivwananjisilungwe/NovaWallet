# 06 · Compliance notes for Zambia

> **This is not legal advice.** It is a checklist of topics to confirm with the Bank of Zambia (BoZ), your licensed partner and a Zambian lawyer before you hold or move real customer money. Laws marked **VERIFY** were recent or changing when this was written (November 2026).

---

## 1. The big decision: licence or licensed partner

Holding customer money in a wallet and moving it is a regulated payment and e-money activity in Zambia. You have two paths:

| Path | What it means | Fit for NovaWallet now |
|---|---|---|
| **A. Partner model (recommended to start)** | A licensed bank or e-money issuer holds the customer funds (in a trust/pooled account) and is the regulated entity; NovaWallet is the technology and customer front-end under their licence/agreement | Yes: fastest and cheapest legal way to launch a pilot |
| B. Own licence | Apply to BoZ as a payment system business / e-money issuer | Later: needs capital, compliance staff, audited systems |

**Until path A or B is in place:** run only in **sandbox/demo mode** with demo money labelled "Demo" (the design supports this), or as a budgeting/tracking app that doesn't hold funds.

## 2. Laws and rules to check

| Area | Instrument | What to check |
|---|---|---|
| Payment systems | National Payment Systems Act (new 2026 Act, VERIFY commencement; earlier NPS Act 2007 until then) and BoZ directives | Which licence or designation covers wallet + payouts; partner obligations |
| E-money | BoZ E-Money Directives (2023) | Trust/pooled account rules, real-time monitoring of holding accounts, agent rules, wallet limits by KYC tier, reporting |
| Banking | Banking and Financial Services Act (2026 update, VERIFY) | Whether any activity needs a banking licence; partner bank role |
| AML/CFT | Financial Intelligence Centre Act (and amendments) | Customer due diligence (KYC tiers), suspicious transaction reports to the FIC, record keeping, sanctions screening, PEP checks |
| Data protection | Data Protection Act, 2021 | Register with the Office of the Data Protection Commissioner, lawful basis/consent, data subject rights (O12 export), breach notification, cross-border transfer rules (hosting and the AI provider) |
| Cyber | Cyber Security and Cyber Crimes laws (2021 framework; VERIFY updates) | Security incident duties |
| Consumer protection | Competition and Consumer Protection Act + BoZ market conduct rules | Clear fees before confirming (designed), complaint handling times (support cases), fair terms |
| Telecoms / SMS | ZICTA rules | SMS sender ID registration, short codes, number ranges |
| Cards | Card network rules via the issuer partner; PCI DSS | Doc 04 |

## 3. How the design already supports compliance

| Requirement | Where it is handled |
|---|---|
| KYC tiers and limits | D02 levels; limits from server (`/kyc/tiers`); admin X11 with maker-checker |
| Identity verification | D03–D09 NRC + selfie + reviewer (X05/X06) |
| Transaction monitoring | Risk scoring (AI doc C1), admin alerts, reconciliation (X09) |
| Record keeping | Immutable ledger + audit log (X14); statements (J05); close-account rules (O13) |
| Fee transparency | Fees shown before every confirm (F02, F04, G07, I01, K02) |
| Complaints | Report a problem (J06), support chat (O11), admin cases (X13) with timestamps |
| Data rights | Privacy screen (O12): consents, download my data |
| No PIN collection | Customers approve mobile money on their own phone; staff warned never to ask for PINs |
| Truthful marketing | Don't claim "data stays in Zambia" or "licensed" unless true (A05) |

## 4. Before the pilot: checklist

- [ ] Partner (bank/e-money issuer) agreement signed; pooled/trust account opened.
- [ ] Partner and BoZ confirm the product is allowed (no objection / approval as required).
- [ ] AML policy, KYC policy and risk assessment written; a compliance officer named (can be part-time at first).
- [ ] FIC reporting process set up (who files suspicious transaction reports and how).
- [ ] Data protection registration done; privacy notice and terms published (in-app web view).
- [ ] Hosting and AI providers checked for data transfer rules; data processing agreements signed.
- [ ] Penetration test done and critical findings fixed.
- [ ] Complaint handling SLA published (e.g. first reply within 24 h).
- [ ] Limits configured to partner/BoZ values (replace the placeholders in D02/X11).

## 5. Students under 18

Some university students are 16 or 17. Minors usually need guardian consent for financial accounts. Decide with your partner and lawyer whether to:

- set the minimum age at 18, or
- allow 16–17 with lower limits and guardian consent.

Then enforce the choice at sign-up (B03 date of birth) and in KYC.
