# NovaWallet Differentiator Features

> Features Zazu doesn't have — these make NovaWallet unique

---

## Feature 1: Chilemba / Group Savings

### What it is
A digital merry-go-round savings group. In Zambia, groups of friends/colleagues pool money — each member contributes a fixed amount weekly/monthly, and one member takes the whole pot each cycle. It's called chilemba, icibemba, or village banking.

Right now it's managed on paper or WhatsApp. NovaWallet digitizes it.

### How it works (user flow)

```
Step 1: User creates a group
  → Opens NovaWallet → taps "Groups"
  → "Create Savings Group"
  → Sets: Group name ("Mulungushi Friends"), 
           Contribution amount (K200),
           Frequency (weekly),
           Number of members (5),
           Cycle length (5 weeks — each member gets paid once)
  → Invites members via phone number or NovaWallet username

Step 2: Members join
  → Each member receives notification
  → They tap "Join Group"
  → They accept terms: "I agree to contribute K200/week for 5 weeks"
  → Their wallet is linked to the group

Step 3: Contributions happen automatically
  → Every Monday at 8am, NovaWallet checks each member's balance
  → If balance >= K200: auto-deduct and add to group pot
  → If balance < K200: member gets notification "Your contribution is due. 
       Add money to your wallet before end of day."
  → Late payment: member gets a "missed contribution" mark

Step 4: Payout rotation
  → Week 1: Member A receives the full pot (K200 × 5 = K1,000)
  → Week 2: Member B receives K1,000
  → Week 3: Member C receives K1,000
  → Week 4: Member D receives K1,000
  → Week 5: Member E (group organizer) receives K1,000
  → NovaWallet sends notification each payout:
       "🎉 You received K1,000 from Mulungushi Friends group!"

Step 5: Cycle complete
  → Group can start a new cycle
  → Members see their savings history
  → Group organizer gets a summary report
```

### Backend logic

```
Group model:
  name, description
  contributionAmount (K200)
  frequency (weekly / biweekly / monthly)
  memberCount
  cycleLength (how many weeks until everyone is paid)
  status (active / completed / cancelled)
  currentCycle (1, 2, 3...)
  organizerId (User ref)

GroupMember model:
  groupId
  userId
  joinDate
  totalContributions (calculated)
  totalPayouts (calculated)
  missedContributions (count)
  payoutOrder (1, 2, 3... — determines who gets paid when)

Contribution model:
  groupId
  memberId
  amount
  dueDate
  paidDate (null if missed)
  status (paid / missed / pending)

Payout model:
  groupId
  memberId
  amount
  weekNumber
  status (pending / paid)
  paidDate
```

### Why this is huge for Zambia

- **Chilemba is everywhere** — every workplace, church group, and friend circle runs one
- **Currently all manual** — someone collects cash, records on paper, chases defaulters
- **Default rate is high** — because there's no accountability
- **NovaWallet solves**: auto-deduction (no chasing), transparent records (everyone sees), automated payouts (no human error)

### Revenue from this

| Method | How |
|---|---|
| Group creation fee | K5-10 per group created |
| Late payment penalty | K5 per missed contribution (goes to group pot) |
| Premium groups | K20/month for priority support + custom rules |

---

## Feature 2: School Fee Payment Tracking

### What it is
Parents can pay school fees directly from NovaWallet to schools. Schools get a dashboard to track who has paid. Parents can see their payment history.

### How it works

```
Step 1: School registers on NovaWallet
  → School fills: name, location, bank account details
  → NovaWallet creates a "School Merchant" account
  → School gets a unique payment code: NW-SCH-001

Step 2: Parent adds their child's school
  → Opens NovaWallet → taps "Pay School"
  → Searches school name or enters school code
  → Enters: student name, grade/class, student ID (if applicable)
  → School is saved to parent's profile

Step 3: Parent pays fees
  → Parent selects the school → sees terms (term 1, term 2, etc.)
  → Enters amount: K2,500
  → Enters PIN
  → Money moves from parent's wallet to school's wallet
  → Parent receives receipt
  → School receives notification: "John Banda paid K2,500 for Mary Banda — Grade 10"

Step 4: School admin dashboard
  → School logs into web dashboard
  → Sees list of all payments: student name, amount, date, term
  → Can mark a student as "fully paid" for the term
  → Can export report for their records
```

### Backend logic

```
SchoolMerchant model:
  name, address, contactPerson, contactPhone
  walletId (they have a NovaWallet merchant wallet)
  status (pending / approved / active)
  registrationDate

SchoolPayment model:
  parentId (User ref)
  schoolId (SchoolMerchant ref)
  studentName, studentGrade, studentId
  term (1 / 2 / 3)
  amount
  receiptNumber
  status (pending / confirmed)
  createdAt
```

### Why this works in Zambia

- **School fee payment is every parent's stress** — queuing at banks, getting receipts, tracking what's paid
- **Schools struggle** — tracking 1,000+ students' payments on paper is chaos
- **NovaWallet gives**: instant payment, instant receipt, full history for parent AND school

### Revenue

| Method | How |
|---|---|
| School registration fee | K200-500 one-time (per school) |
| Transaction fee | 0.5-1% of school fee payment (parent pays) |
| School dashboard subscription | K100-300/month for the school to access reporting |

---

## Feature 3: Shared Expenses / Split Bills

### What it is
A group of friends can create a shared expense (road trip, party, group project) and NovaWallet tracks who paid what and who owes whom. Automatically calculates settlements.

### How it works

```
Scenario: 4 friends drive from Lusaka to Livingstone for the weekend.

Step 1: Create the trip/event
  → Opens NovaWallet → taps "Split"
  → Creates "Livingstone Trip"
  → Adds members: Bwalya, Chanda, Mulenga, yourself
  → Total budget estimate: K2,000

Step 2: Record expenses as they happen
  → Bwalya pays for fuel: K800
     → Opens the trip → "Add Expense"
     → Amount: K800, paid by: Bwalya, split between: everyone (4)
  → Chanda pays for accommodation: K600
     → Same: "Add Expense" → K600, paid by Chanda, split between: everyone (4)
  → You pay for food: K400
     → Same: K400, paid by you, split between: everyone (4)

Step 3: NovaWallet calculates who owes whom
  → Total spent: K1,800
  → Each person's share: K450
  → Bwalya paid K800, should have paid K450 → overpaid K350 → others owe him
  → Chanda paid K600, should have paid K450 → overpaid K150 → others owe her
  → You paid K400, should have paid K450 → underpaid K50 → you owe K50
  → Mulenga paid K0, should have paid K450 → underpaid K450 → he owes K450

Step 4: Settle up
  → NovaWallet shows: "Settle up summary"
  → You: "Send K50 to Bwalya" — tap to send
  → Mulenga: "Send K350 to Bwalya, Send K150 to Chanda" — tap to send both
  → All payments go through NovaWallet transfers

Step 5: Everyone gets a summary receipt
  → "Livingstone Trip — Final Summary"
  → Each person sees what they paid, what they owe, and to whom
```

### Backend logic

```
SharedExpenseGroup model:
  name, description
  createdBy (User ref)
  members (array of User refs)
  totalSpent (calculated)
  status (active / settled)

Expense model:
  groupId
  paidBy (User ref)
  amount
  description ("Fuel", "Accommodation")
  splitType (equal / custom)
  createdAt

Settlement model:
  groupId
  fromUserId (owes money)
  toUserId (is owed money)
  amount
  status (pending / paid)
```

### Why this works for students

- **Students share everything** — trips, rent, groceries, group project materials
- **Current solution**: "I'll send you the money later" → forgotten, awkward conversations
- **NovaWallet solves**: clear record of who owes what, one-tap settlement, no awkwardness

### Revenue

| Method | How |
|---|---|
| Free for basic (up to 5 expenses per group) | User acquisition |
| Premium split (K5/group) | Unlimited expenses, PDF reports |
| Settlement fee | K1 per settlement (tiny, adds up with volume) |

---

## Comparison: NovaWallet vs Zazu

| Feature | Zazu | NovaWallet | Differentiator? |
|---|---|---|---|
| Digital wallet | ✅ | ✅ | No |
| Virtual cards | ✅ | ✅ | No |
| Mobile money deposit/withdraw | ✅ | ✅ | No |
| Free transfers | ✅ | ✅ | No |
| Spending categorization | ✅ | ✅ | No |
| Bill payments (ZESCO, airtime) | ✅ | ❌ | Zazu wins |
| QR payments | ✅ | ❌ | Zazu wins |
| **Chilemba / Group savings** | ❌ | ✅ | **YES — NovaWallet unique** |
| **School fee payments** | ❌ | ✅ | **YES — NovaWallet unique** |
| **Shared expenses / Split bills** | ❌ | ✅ | **YES — NovaWallet unique** |

---

## Which one should you build?

| If you want to... | Build this feature |
|---|---|
| Solve a real problem Zambians have every day | **Chilemba / Group savings** — biggest pain point |
| Impress parents and schools | **School fee payments** — every parent will download your app |
| Help students (like yourself) | **Shared expenses / Split bills** — perfect for campus |
| Have the most unique offering | **All three** — Zazu does none of them |

**My recommendation:** Build the wallet core first (deposit, transfer, withdraw) — that's your foundation. Then add **Chilemba/Group Savings** as your headline feature. That alone makes NovaWallet different from Zazu.
