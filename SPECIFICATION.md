# NovaWallet — Full Specification

> Digital wallet + virtual cards + mobile money integration

---

## What NovaWallet Is

A digital wallet backend + mobile app that:
- Manages users and wallets
- Processes deposits, withdrawals, and transfers
- Issues virtual cards for online payments
- Integrates with mobile money (Airtel/MTN)
- Enforces banking rules (no negative balance, transaction PIN, KYC limits, atomic transfers)
- Later connects to real payment providers (Union54, Flutterwave)

---

## Core Features by Phase

| V1 (Your Project) | V2 (After Launch) | V3 (Scale) |
|---|---|---|
| Register/Login with JWT | Africa's Talking mobile money | Real Union54 card issuing |
| Wallet with balance | Transaction categorization (AI) | ML fraud detection |
| Deposit (mock) + Withdraw (mock) | Beneficiaries/contacts | Credit scoring |
| Transfer between users | QR payments | Multi-country (Flutterwave) |
| Transaction history + references | 2-factor authentication | Merchant accounts |
| Transaction PIN | Real mobile money webhooks | Bank account integration |
| KYC Level 1 (name + phone + NRC) | Spending insights | Interest on balances |
| Simulated virtual cards | KYC Level 2 (documents) | AI customer support |
| Admin panel (confirm deposits) | Admin panel (full) | |
| Rule-based fraud alerts | | |

---

## Database Models

### User
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | Auto-generated |
| name | String | Required |
| email | String | Required, unique |
| phone | String | Required, unique |
| nrcNumber | String | National Registration Card |
| password | String | bcrypt hashed |
| transactionPin | String | bcrypt hashed, separate from password |
| kycLevel | Number | 0 (none), 1 (basic), 2 (full) |
| kycStatus | String | pending / verified / rejected |
| role | String | user / admin |
| fcmToken | String | Push notifications |
| createdAt | Date | Auto |

### Wallet
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| userId | ObjectId | 1:1 with User |
| accountNumber | String | Generated: NW-YYYYMM-NNNN |
| balance | Number | Calculated from ledger, never set directly |
| currency | String | ZMW |
| status | String | active / frozen / closed |
| linkedMobileMoney | Object | { phone, provider } |
| dailyLimit | Number | Based on KYC level |
| createdAt | Date | |

### LedgerEntry (the REAL balance)
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| walletId | ObjectId | Which wallet |
| type | String | deposit / withdraw / transfer / fee / reversal |
| amount | Number | Positive or negative |
| beforeBalance | Number | Balance before this entry |
| afterBalance | Number | Balance after this entry |
| reference | String | Unique, human-readable |
| description | String | |
| counterpartyType | String | user / external |
| counterpartyId | ObjectId | Other wallet or null |
| status | String | pending / completed / failed |
| createdAt | Date | |

### Transaction
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| reference | String | NW-YYYYMMDD-XXXXX |
| type | String | deposit / withdraw / transfer / card_fee |
| senderWalletId | ObjectId | Null for deposits |
| receiverWalletId | ObjectId | Null for withdrawals |
| amount | Number | In ZMW |
| fee | Number | Platform fee |
| description | String | User-provided |
| status | String | pending / completed / failed / reversed |
| pinVerified | Boolean | Was PIN entered correctly |
| createdAt | Date | |
| completedAt | Date | Null until completed |

### VirtualCard
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| walletId | ObjectId | Owner |
| name | String | User-defined name |
| cardNumber | String | Full number (shown once, then masked) |
| last4 | String | Last 4 digits only |
| expiryMonth | Number | |
| expiryYear | Number | |
| cvvHash | String | Hashed, never stored in plain text |
| status | String | active / frozen / closed |
| spendingLimit | Number | Monthly limit in ZMW |
| spentThisMonth | Number | Running total |
| createdAt | Date | |

### MobileMoneyRequest
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| userId | ObjectId | |
| walletId | ObjectId | |
| type | String | deposit / withdraw |
| provider | String | airtel / mtn |
| phone | String | User's mobile money number |
| amount | Number | |
| fee | Number | |
| reference | String | Internal reference |
| providerReference | String | From callback (null until completed) |
| status | String | pending / completed / failed |
| createdAt | Date | |
| completedAt | Date | |

### KycDocument
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| userId | ObjectId | |
| type | String | nrc_front / nrc_back / selfie / proof_of_address |
| fileUrl | String | Cloudinary URL |
| status | String | pending / verified / rejected |
| rejectionReason | String | If rejected |
| uploadedAt | Date | |
| verifiedAt | Date | |

### Beneficiary
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| userId | ObjectId | Who saved this |
| name | String | Display name |
| phone | String | |
| accountNumber | String | NovaWallet account |
| createdAt | Date | |

### AuditLog
| Field | Type | Notes |
|---|---|---|
| id | ObjectId | |
| userId | ObjectId | Who did it |
| action | String | login / password_change / pin_change / transfer / freeze_card / etc |
| details | Object | JSON — extra info about the action |
| ipAddress | String | |
| userAgent | String | Browser/device info |
| createdAt | Date | |

---

## API Routes

### Auth
```
POST   /api/auth/register            → Create account
POST   /api/auth/login               → Login, returns JWT
POST   /api/auth/set-pin             → Set transaction PIN
PUT    /api/auth/change-password     → Change login password
PUT    /api/auth/change-pin          → Change transaction PIN
PUT    /api/auth/change-phone        → Change phone number
GET    /api/auth/me                  → Get current user + wallet
```

### Wallet
```
GET    /api/wallet                   → Get wallet balance + account number
GET    /api/wallet/ledger            → Get ledger entries (audit trail)
```

### Transactions
```
POST   /api/transactions/deposit     → Initiate deposit (creates pending request)
POST   /api/transactions/withdraw    → Initiate withdrawal (creates pending request)
POST   /api/transactions/transfer    → Send money to another user
GET    /api/transactions             → Get user's transaction history (?page, ?limit, ?type)
GET    /api/transactions/:id         → Get single transaction detail
POST   /api/transactions/request     → Request money from someone
```

### Virtual Cards
```
POST   /api/cards                    → Create virtual card
GET    /api/cards                    → List user's cards
GET    /api/cards/:id                → Card details (masked number)
GET    /api/cards/:id/transactions   → Transactions on this card
PUT    /api/cards/:id/freeze         → Freeze card
PUT    /api/cards/:id/unfreeze       → Unfreeze card
PUT    /api/cards/:id/limit          → Change spending limit
DELETE /api/cards/:id                → Close card
```

### Beneficiaries
```
POST   /api/beneficiaries            → Save a beneficiary
GET    /api/beneficiaries            → List saved beneficiaries
DELETE /api/beneficiaries/:id        → Remove beneficiary
```

### KYC
```
POST   /api/kyc/upload               → Upload KYC document
GET    /api/kyc/status               → Get KYC status
```

### Admin
```
GET    /api/admin/stats              → Dashboard stats
GET    /api/admin/users              → All users
PUT    /api/admin/users/:id/ban      → Ban/unban user
GET    /api/admin/deposits/pending   → Pending deposits
PUT    /api/admin/deposits/:id/confirm → Confirm deposit
PUT    /api/admin/deposits/:id/reject  → Reject deposit
GET    /api/admin/withdrawals/pending → Pending withdrawals
PUT    /api/admin/withdrawals/:id/confirm → Confirm withdrawal
GET    /api/admin/transactions       → All transactions
GET    /api/admin/cards              → All cards
```

---

## Revenue Model

| Stream | Details | Projected Income |
|---|---|---|
| Transfer fee | Free (V1), 0.5% later | Grows with volume |
| Withdrawal fee | 1-2% + K2 flat | **Primary revenue** |
| Card issuance | K30 one-time | Per new card |
| Card transaction fee | 1.5-2.5% per payment | Secondary |
| Merchant accounts | K100-300/month | After Phase 3 |

---

## Costs

| Item | Cost |
|---|---|
| Learning + building on laptop | K0 |
| Hosting (Railway/Heroku free tier) | K0 |
| Database (MongoDB Atlas free) | K0 |
| Cloudinary (KYC docs) | K0 |
| Google Play Store (optional) | $25 one-time |
| Africa's Talking SMS (production) | K200-500 prepaid |

**Total to build and demo: K0**
**Total to launch on Play Store: ~K350 ($25)**

---

## Tech Stack Options

| Layer | Option A (Java) | Option B (JS) |
|---|---|---|
| Backend | Spring Boot + Spring Security + JPA | Express.js + Mongoose |
| Database | PostgreSQL (Neon free tier) | MongoDB (Atlas free tier) |
| Auth | Spring Security + JWT | jsonwebtoken |
| Mobile | Flutter / KMP | Flutter |
| Hosting | Railway / Render / Oracle Cloud | Railway / Render |

---

## Build Order (V1)

1. User registration + login + JWT
2. Transaction PIN setup
3. Wallet model + account number generation
4. Deposit (mock — admin confirms)
5. Transfer between users (atomic — both succeed or fail)
6. Transaction history + reference numbers
7. Withdrawal (mock — admin confirms)
8. Virtual cards (simulated)
9. KYC Level 1 (name + phone + NRC)
10. Admin panel (confirm deposits/withdrawals)
11. Rule-based fraud alerts
12. Beneficiaries
