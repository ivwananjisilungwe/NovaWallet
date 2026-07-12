# NovaWallet — Complete User Flow

> From download to first transaction. Every screen, every step.

---

## The Four Tab Structure

```
Bottom Navigation:
┌──────────┬──────────┬──────────┬──────────┐
│  Home    │  Send    │  Cards   │  Profile │
│  (Wallet)│  (Money) │ (Virtual)│  (Settings)│
└──────────┴──────────┴──────────┴──────────┘
```

---

## Complete Walkthrough — New User

### Step 1: First Launch — Onboarding

```
User opens the app for the first time.
  → Sees 3 onboarding screens (swipeable):

  Screen 1: "Your Digital Wallet"
    Illustration of a phone with money
    "Send, receive, and manage your money — all from your phone"

  Screen 2: "Virtual Cards"
    Illustration of a card
    "Create virtual cards for online payments anywhere in the world"

  Screen 3: "Secure & Simple"
    Illustration of a lock
    "Your money is protected with PIN and encryption"

  Bottom: "Get Started" button
```

---

### Step 2: Create Account

```
User taps "Get Started"
  → Goes to Registration screen:

  ┌─────────────────────────────────┐
  │     Create Your Account         │
  │                                 │
  │  Full Name                      │
  │  [________________________]     │
  │                                 │
  │  Email Address                  │
  │  [________________________]     │
  │                                 │
  │  Phone Number                   │
  │  [+260 97_ ___ ____]           │
  │                                 │
  │  NRC/Passport Number            │
  │  [________________________]     │
  │                                 │
  │  Create Password                │
  │  [________________________]     │
  │  (Must be 8+ characters)        │
  │                                 │
  │  [✓] I agree to Terms & Conditions
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Create Account      │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  Already have an account? Login │
  └─────────────────────────────────┘

User fills in details and taps "Create Account".

BACKEND:
  POST /api/auth/register
  { name, email, phone, nrc, password }
  
  Backend:
    1. Validate input (name, valid email, phone format, password strength)
    2. Check email not already registered
    3. Check phone not already registered
    4. Hash password (bcrypt)
    5. Create User record
    6. Create Wallet record (generate account number, balance = 0)
    7. Sign JWT token
    8. Return { token, user, wallet }

APP:
    1. Save JWT token securely
    2. Navigate to "Set Transaction PIN" screen
```

---

### Step 3: Set Transaction PIN

```
  ┌─────────────────────────────────┐
  │     Set Your Transaction PIN    │
  │                                 │
  │  This PIN protects your money.  │
  │  You'll enter it for every      │
  │  transfer and withdrawal.       │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │    [_][_][_][_]         │   │
  │  │     Enter 4-digit PIN   │   │
  │  │                         │   │
  │  │    [_][_][_][_]         │   │
  │  │     Confirm PIN         │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │      Set PIN            │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

BACKEND:
  POST /api/auth/set-pin
  { pin }  (hashed before storage)

  Backend:
    1. Hash the PIN (bcrypt, separate from password)
    2. Save to User record
    3. Mark user as "onboarding complete"

APP:
  Navigate to KYC screen (or skip for now)
```

---

### Step 4: KYC (Know Your Customer)

```
User can skip this now and do it later.
But if they want higher limits later, they'll need it.

  ┌─────────────────────────────────┐
  │     Verify Your Identity        │
  │                                 │
  │  Level 1 (recommended now):     │
  │  ✓ Name ✓ Phone ✓ NRC          │
  │                                 │
  │  Level 2 (do later):            │
  │  ☐ Selfie with NRC              │
  │  ☐ Proof of address             │
  │                                 │
  │  Your current limit: K0/day     │
  │  After Level 1: K5,000/day      │
  │  After Level 2: K50,000/day     │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │   Complete Later        │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

User taps "Complete Later" → goes to Home screen.
```

---

### Step 5: Home Screen (Wallet Dashboard)

```
  ┌─────────────────────────────────┐
  │  NW - Extract your wealth       │
  │                                 │
  │  Available Balance              │
  │  ┌─────────────────────────┐   │
  │  │      ZMK 0.00           │   │
  │  │                         │   │
  │  │  [Deposit]  [Withdraw]  │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  Account Number: NW-202606-001  │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │  Quick Actions           │   │
  │  │  ┌─────┐ ┌─────┐ ┌───┐ │   │
  │  │  │Send │ │Request│ │Buy│ │   │
  │  │  │     │ │       │ │Air│ │   │
  │  │  └─────┘ └─────┘ └───┘ │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  Recent Transactions            │
  │  ┌─────────────────────────┐   │
  │  │  No transactions yet    │   │
  │  │                         │   │
  │  │  Tap "Deposit" to add   │   │
  │  │  money to your wallet   │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  [Home] [Send] [Cards] [Profile]│
  └─────────────────────────────────┘
```

---

### Step 6: Deposit Money (First Real Action)

```
User taps "Deposit"

  ┌─────────────────────────────────┐
  │     Add Money                   │
  │                                 │
  │  How much do you want to add?   │
  │  [_____K_______]                │
  │                                 │
  │  Payment Method:                │
  │  ○ Airtel Money                 │
  │  ○ MTN Mobile Money             │
  │  ○ Bank Transfer                │
  │                                 │
  │  (User selects Airtel Money)    │
  │                                 │
  │  Airtel Money Number:           │
  │  [+260 97 ___ ____]            │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Continue            │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

User enters K100, selects Airtel Money.

BACKEND:
  POST /api/payments/mobile-money/initiate
  { amount: 100, phone: "+26097xxxxxx", provider: "airtel" }

  Backend (V1 — mock mode):
    1. Create MobileMoneyRequest record (status: "pending")
    2. Return: { reference: "DEP-20260626-001", status: "pending" }
    
  APP:
    Shows: "Payment request sent. Check your phone."

```

### Step 6b: User confirms by sending money (mock flow)

```
Since we're running in mock mode (V1):

  ┌─────────────────────────────────┐
  │     Deposit Requested           │
  │                                 │
  │  Amount: K100                   │
  │  Reference: DEP-20260626-001    │
  │  Status: Pending                │
  │                                 │
  │  Please send K100 to            │
  │  NovaWallet Airtel Money:       │
  │  +260 97 123 4567               │
  │                                 │
  │  Or wait for admin to confirm   │
  │  your payment.                  │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │   I've Sent the Money   │   │ ← User taps this
  │  └─────────────────────────┘   │
  │                                 │
  │  (Admin receives notification,  │
  │   confirms in admin panel,      │
  │   wallet is credited)           │
  └─────────────────────────────────┘

BACKEND (admin confirms):
  PUT /api/admin/deposits/:id/confirm
  
  Backend:
    1. Find MobileMoneyRequest
    2. Create LedgerEntry (type: "deposit", amount: +100)
    3. Update wallet balance (calculated from ledger)
    4. Create Transaction record (reference, status: "completed")
    5. Send push notification to user

APP:
  Notification: "K100 deposited successfully!
                  New balance: K100.00"
```

---

### Step 7: Send Money (Transfer)

```
User taps "Send" tab

  ┌─────────────────────────────────┐
  │     Send Money                  │
  │                                 │
  │  Recipient                      │
  │  [________________________]     │
  │  (Phone number or NW account)   │
  │                                 │
  │  Amount                         │
  │  [_____K_______]                │
  │                                 │
  │  Description (optional)         │
  │  [________________________]     │
  │  e.g. "School fees", "Lunch"    │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Continue            │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

User enters recipient's phone: +260 97 765 4321
Amount: K50
Description: "Textbook payment"

  ┌─────────────────────────────────┐
  │     Confirm Transfer            │
  │                                 │
  │  From: NovaWallet (K100.00)     │
  │  To: +260 97 765 4321           │
  │  Amount: K50.00                 │
  │  Fee: K0.00 (free)              │
  │  Total: K50.00                  │
  │                                 │
  │  Enter PIN:                     │
  │  ┌─────────────────────────┐   │
  │  │    [_][_][_][_]         │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Send K50            │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

User enters PIN, taps "Send K50".

BACKEND:
  POST /api/transactions/transfer
  { recipient: "+260977654321", amount: 50, description: "Textbook payment", pin: "1234" }

  Backend:
    1. Verify PIN (hash match)
    2. Find recipient by phone number
    3. Validate: sender has sufficient balance (K100 >= K50)
    4. Validate: sender != recipient
    5. Validate: no negative balance after
    6. BEGIN DATABASE TRANSACTION
       a. Create LedgerEntry for sender (type: "transfer", amount: -50, before: 100, after: 50)
       b. Create LedgerEntry for receiver (type: "transfer", amount: +50, before: 0, after: 50)
       c. Create Transaction record (reference: "NW-20260626-002", status: "completed")
       d. Create notifications for both users
    7. COMMIT DATABASE TRANSACTION
       (if any step fails, everything rolls back)
    
  APP:
    ✓ Shows success screen
    ✓ Sender gets notification: "Sent K50 to +260 97 765 4321"
    ✓ Receiver gets notification: "Received K50 from you"
```

### Success Screen

```
  ┌─────────────────────────────────┐
  │                                 │
  │        ✓                        │
  │    Payment Successful!          │
  │                                 │
  │  K50.00 sent to                 │
  │  +260 97 765 4321               │
  │                                 │
  │  Reference: NW-20260626-002     │
  │  New Balance: K50.00            │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Done                │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Share Receipt       │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘
```

---

### Step 8: Create Virtual Card

```
User taps "Cards" tab

  ┌─────────────────────────────────┐
  │     Your Cards                  │
  │                                 │
  │  You don't have any cards yet.  │
  │                                 │
  │  Create a virtual card for      │
  │  online payments.               │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │    Create Virtual Card  │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

User taps "Create Virtual Card".

  ┌─────────────────────────────────┐
  │     Create Virtual Card         │
  │                                 │
  │  Card Name                      │
  │  [My Shopping Card________]     │
  │                                 │
  │  Spending Limit                 │
  │  [K_5,000______] (per month)   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │  Create Card (K30 fee)  │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  Balance: K50.00                │
  │  Fee: K30.00                    │
  │  After: K20.00                  │
  └─────────────────────────────────┘

User confirms.

BACKEND:
  POST /api/cards/create
  { name: "My Shopping Card", spendingLimit: 5000 }

  Backend (simulated card):
    1. Validate balance >= K30 (fee)
    2. Deduct K30 from wallet
    3. Generate card number using Luhn algorithm:
       4 randomly generated digits
       Check digit calculated
       Format: 5234 5678 9012 3456
    4. Generate random expiry (3 years from now)
    5. Generate random CVV (3 digits, hashed for storage)
    6. Create Card record
    7. Return: last4, expiry, masked card number
       (full PAN and CVV shown ONCE, then never again)

  APP (card created screen):
    
  ┌─────────────────────────────────┐
  │     Card Created!               │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │                         │   │
  │  │  ┌───┐ ┌───┐ ┌───┐ ┌─┐│   │
  │  │  │52 │ │34 │ │56 │ │78││   │
  │  │  └───┘ └───┘ └───┘ └─┘│   │
  │  │                         │   │
  │  │  Valid Thru: 06/29      │   │
  │  │  CVV: 123               │   │
  │  │  Status: Active         │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ⚠️ Save these details.        │
  │  We won't show the full         │
  │  card number again.             │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │   Copy Card Details     │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Done                │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘
```

### Cards Tab — After creating

```
  ┌─────────────────────────────────┐
  │     Your Cards                  │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │  My Shopping Card       │   │
  │  │  **** **** **** 3456    │   │
  │  │  Exp: 06/29  Status: ✅ │   │
  │  │  Limit: K5,000/mo       │   │
  │  │  Used: K0               │   │
  │  │                         │   │
  │  │  [Freeze] [Limits] [Del]│   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │   Create New Card       │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

Tapping "Freeze": card becomes inactive until unfrozen.
Tapping "Limits": adjust monthly spending limit.
Tapping card: see transaction history on this card.
```

---

### Step 9: Withdraw Money

```
User taps "Withdraw" from Home screen.

  ┌─────────────────────────────────┐
  │     Withdraw to Mobile Money    │
  │                                 │
  │  Amount                         │
  │  [_____K_______]                │
  │                                 │
  │  Withdraw to:                   │
  │  ○ Airtel Money                 │
  │     +260 97 xxx xxx             │
  │  ○ MTN Mobile Money             │
  │     +260 97 xxx xxx             │
  │                                 │
  │  Fee: 2% + K2 = K3              │
  │  You receive: K97               │
  │                                 │
  │  Enter PIN:                     │
  │  ┌─────────────────────────┐   │
  │  │    [_][_][_][_]         │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │   Withdraw K100         │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘

BACKEND (V1 — mock mode):
  POST /api/transactions/withdraw
  { amount: 100, provider: "airtel", pin: "1234" }

  Backend:
    1. Verify PIN
    2. Check balance >= amount + fee
    3. Create withdrawal request (status: "pending")
    4. Deduct from wallet (earmark the funds)
    
  APP:
    Shows: "Withdrawal requested. Admin will send K97 to your Airtel Money."

  (Admin manually sends money via their own Airtel Money,
   confirms in admin panel → wallet officially deducted)
```

---

### Step 10: Transaction History

```
User can view transaction history from Home screen
by tapping "View All" or scrolling down.

  ┌─────────────────────────────────┐
  │     Transaction History         │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │  Today                  │   │
  │  ├─────────────────────────┤   │
  │  │  Sent to +260977654321  │   │
  │  │  -K50.00     ✓ Success  │   │
  │  │  2:30 PM  REF: NW-00...│   │
  │  ├─────────────────────────┤   │
  │  │  Card Created           │   │
  │  │  -K30.00     ✓ Success  │   │
  │  │  2:15 PM  My Shopping...│   │
  │  ├─────────────────────────┤   │
  │  │  Deposit (Airtel Money) │   │
  │  │  +K100.00    ✓ Success  │   │
  │  │  1:00 PM  REF: DEP-0...│   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │  Yesterday              │   │
  │  ├─────────────────────────┤   │
  │  │  Withdrawal to Airtel   │   │
  │  │  -K50.00     ✓ Success  │   │
  │  │  10:00 AM  REF: WIT-0..│   │
  │  └─────────────────────────┘   │
  │                                 │
  │  Tap any transaction for more   │
  │  details.                       │
  └─────────────────────────────────┘

Tapping a transaction:

  ┌─────────────────────────────────┐
  │     Transaction Details         │
  │                                 │
  │  Reference: NW-20260626-002     │
  │  Type: Transfer                 │
  │  Status: Completed              │
  │                                 │
  │  From: NovaWallet               │
  │  To: +260 97 765 4321           │
  │  (John Banda)                   │
  │                                 │
  │  Amount: K50.00                 │
  │  Fee: K0.00                     │
  │  Total: K50.00                  │
  │                                 │
  │  Description: Textbook payment  │
  │                                 │
  │  Date: 26 June 2026, 2:30 PM   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Share Receipt       │   │
  │  └─────────────────────────┘   │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │     Report an Issue     │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘
```

---

### Step 11: Profile / Settings

```
  ┌─────────────────────────────────┐
  │     Profile                     │
  │                                 │
  │  John Banda                     │
  │  john@email.com                 │
  │  +260 97 xxx xxxx               │
  │                                 │
  │  KYC Level: 1 (Basic)          │
  │  [Upgrade to Level 2]           │
  │                                 │
  │  ┌─────────────────────────┐   │
  │  │  Account Settings        │   │
  │  │  ├ Change Password      │   │
  │  │  ├ Change PIN           │   │
  │  │  ├ Change Phone Number  │   │
  │  │  ├ Limits & Controls    │   │
  │  │  └ Notification Prefs   │   │
  │  ├─────────────────────────┤   │
  │  │  Security               │   │
  │  │  ├ Linked Devices       │   │
  │  │  ├ Login Activity       │   │
  │  │  └ Two-Factor Auth      │   │
  │  ├─────────────────────────┤   │
  │  │  Support                │   │
  │  │  ├ Help Center          │   │
  │  │  ├ Report Problem       │   │
  │  │  └ FAQs                 │   │
  │  ├─────────────────────────┤   │
  │  │  About                  │   │
  │  │  └ Logout               │   │
  │  └─────────────────────────┘   │
  └─────────────────────────────────┘
```

---

## Admin Panel Flow (Your View)

```
Admin logs into a WEB dashboard (not mobile).

┌──────────────────────────────────────────────────┐
│  NovaWallet Admin                     [Logout]   │
│                                                   │
│  ┌─────┐ ┌─────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│  │Users│ │Depos│ │Withdr│ │Txns  │ │Cards │   │
│  └─────┘ └─────┘ └──────┘ └──────┘ └──────┘   │
│                                                   │
│  ┌────────────────────────────────────────────┐   │
│  │  PENDING DEPOSITS                          │   │
│  │────────────────────────────────────────────│   │
│  │  John Banda  +260977xxxxxx  K100  [Confirm]│   │
│  │  Mary Mwale  +260976xxxxxx  K50   [Confirm]│   │
│  │────────────────────────────────────────────│   │
│  │  Bulk Action: [Confirm All Shown]          │   │
│  └────────────────────────────────────────────┘   │
│                                                   │
│  ┌────────────────────────────────────────────┐   │
│  │  PENDING WITHDRAWALS                       │   │
│  │────────────────────────────────────────────│   │
│  │  Peter Banda  Airtel  K200  REF:NW-...    │   │
│  │  └─ Send K200 via Airtel, then confirm    │   │
│  └────────────────────────────────────────────┘   │
│                                                   │
│  Stats: 25 users | K15,000 in wallet              │
│         12 txns today | K4,500 volume              │
└──────────────────────────────────────────────────┘
```

---

## Complete Data Flow Summary

```
REGISTER:
  App → POST /api/auth/register → Backend creates User + Wallet → Returns JWT

SET PIN:
  App → POST /api/auth/set-pin → Backend hashes + stores PIN

DEPOSIT:
  App → POST /api/payments/mobile-money/initiate → creates pending deposit
  Admin confirms → LedgerEntry (+amount) → Wallet balance recalculated
  Transaction recorded → User notified

SEND MONEY:
  App → POST /api/transactions/transfer → Backend verifies PIN
  Checks balance → BEGIN TRANSACTION → LedgerEntry (-sender, +receiver)
  Transaction recorded → COMMIT → Both users notified

CREATE CARD:
  App → POST /api/cards/create → Backend deducts fee
  Generates card numbers → stores hashed CVV → returns card details

WITHDRAW:
  App → POST /api/transactions/withdraw → Backend verifies PIN + balance
  Creates pending withdrawal → Admin sends money → Confirms

VIEW TRANSACTIONS:
  App → GET /api/transactions?page=1 → Returns paginated transaction list
  Each transaction has: type, amount, status, reference, timestamp, counterparty
```

---

## Every Screen the App Has (Complete Map)

```
APP LAUNCH
├── Splash Screen (logo + loading)
│
├── NOT LOGGED IN:
│   ├── Onboarding (3 swipable screens)
│   ├── Login (email + password)
│   └── Register (name, email, phone, NRC, password)
│       └── Set Transaction PIN (4 digits)
│           └── KYC Screen (skip or complete)
│
├── LOGGED IN — MAIN APP (4 tabs):
│   ├── HOME TAB
│   │   ├── Balance card (amount, account number)
│   │   ├── Quick actions (Deposit, Withdraw, Send, Buy Airtime)
│   │   ├── Mini transaction list (last 5)
│   │   └── "View All" → Full transaction history
│   │
│   ├── SEND TAB
│   │   ├── Send to phone/account (enter recipient, amount, description)
│   │   ├── Beneficiaries (saved recipients)
│   │   ├── Request money (ask someone to pay you)
│   │   └── Confirm screen (PIN + summary)
│   │
│   ├── CARDS TAB
│   │   ├── Cards list (each card: last4, status, limit)
│   │   ├── Card detail (transactions on this card)
│   │   ├── Create card (name + limit + fee confirmation)
│   │   └── Manage card (freeze, adjust limit, close)
│   │
│   └── PROFILE TAB
│       ├── Profile info
│       ├── KYC status
│       ├── Settings (password, PIN, phone, notifications)
│       ├── Security (devices, login history, 2FA)
│       ├── Support (help, report, FAQs)
│       └── Logout
│
├── MODAL SCREENS (pop over the tabs):
│   ├── Deposit screen (amount + payment method)
│   ├── Withdraw screen (amount + mobile money number)
│   ├── Enter PIN (for transactions)
│   ├── Transaction detail (full info + share receipt)
│   └── Success/Error screen
│
└── ADMIN WEB DASHBOARD (separate):
    ├── Dashboard (stats, charts)
    ├── Pending deposits
    ├── Pending withdrawals
    ├── All transactions
    ├── All users
    └── All cards
```

---

## What the User Actually Experiences

```
1. Download app (APK from you or Google Play)
2. Open → See "Get Started" screen
3. Tap → Fill name, email, phone, NRC, password
4. Tap → Set 4-digit PIN
5. See KYC → Skip for now
6. See Home screen → balance K0.00
7. Tap "Deposit" → Enter K100 → Select Airtel Money
8. Phone shows: "Send K100 to NovaWallet Airtel +260977xxxxx"
9. User sends K100 via their Airtel Money
10. Admin confirms → balance shows K100.00
11. Tap "Send" → enter friend's phone → K50 → "Textbook payment"
12. Enter PIN → "Payment Successful!" → balance K50.00
13. Friend receives K50 notification
14. Tap "Cards" → "Create Card" → enters name + limit → pays K30 fee
15. Card appears with number, expiry, CVV
16. User uses card details on Amazon/Netflix
17. Tap Profile → see transaction history with every step recorded
```

---

That's the complete user experience — every screen, every tap, every backend call.

Once you start building, you work through these screens one at a time. Start with Step 2 (registration) and go in order. Each screen is a day's work.
