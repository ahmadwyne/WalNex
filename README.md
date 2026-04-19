# Walnex (E Wallet) — Android (Java) + Firebase (Firestore + Cloud Functions + FCM)

**Platform:** Android (native)  
**Language:** Java  
**Backend/Database:** Firebase (Auth + Firestore + Cloud Functions)  
**Notifications:** Firebase Cloud Messaging (FCM) + in-app inbox  
**Document version:** 1.0  
**PRD date:** 2026-04-19

## 1) Purpose & Product Summary
Walnex is a mobile wallet MVP that allows a user to:
- Sign up / sign in using **Phone OTP** (Firebase Auth)
- Complete a basic wallet profile setup (name + phone; avatar optional)
- View wallet balance and transaction history
- Send money to another Walnex user using **phone-number lookup only**
- Add money via **simulated top-up** (for demo/testing)
- Receive and manage notifications:
  - Push notifications (FCM)
  - In-app notification inbox with read/unread and deep links

**Core principle:** all sensitive money movement is executed server-side via **Cloud Functions**. The Android client is never trusted to update balances.

---

## 2) MVP Scope (What we are building now)
### In scope (MVP)
- Authentication & session persistence (FR-1..FR-5)
- User profile setup/edit (FR-6..FR-7)
- Wallet dashboard: balance + recent transactions (FR-8..FR-11)
- Transaction history with receipt detail + filters (FR-10..FR-11)
- P2P transfers (phone-only recipient lookup) (FR-12..FR-16)
- Add money (simulated top-up) (FR-17A)
- Notifications (push + in-app inbox + basic preferences) (FR-19..FR-25)
- Audit logs for sensitive actions (NFR + FR-7, FR-30 direction)

### Explicitly out of scope (for MVP)
- Username recipient lookup
- QR code recipient lookup
- Real payment integration / webhooks (FR-17B)
- Withdraw/cash-out (FR-18)
- Full admin dashboard UI (optional later)
- Complex compliance/KYC

---

## 3) Critical Security & Integrity Requirements (Non-negotiable)
These rules are the backbone of the wallet. The agent must not violate them.

### 3.1 Money movement is server-side only
- The Android client **must not** write to:
  - `wallets/{uid}` (balance)
  - `ledger/{uid}/entries/*`
- All balance changes happen in Cloud Functions using atomic transactions/batches.

### 3.2 Ledger-first accounting
Every transfer/top-up generates:
1) `transactions/{txId}` record (source of truth for receipts)
2) Ledger entry/entries:
   - sender: `debit`
   - receiver: `credit` (for transfer)
   - only `credit` (for top-up)
3) Wallet balances updated atomically with ledger write(s)

### 3.3 Atomicity
Transfers must be atomic:
- Debit sender + credit receiver + write all records as one atomic server operation.
- No partial updates.

### 3.4 Idempotency (prevents double-debits)
Every money movement request includes `idempotencyKey`.
- If the client retries due to timeout, or the user double-taps confirm, the server must not create duplicate transfers.
- The server must detect “already processed” by `(senderUid, idempotencyKey)` or a dedicated idempotency record.

### 3.5 Firestore Security Rules: least privilege
- Users can read their own data.
- Users cannot read other users’ profiles unless explicitly required (MVP: recipient search happens via phone mapping / server resolution, not by open profile reads).
- Writes are strictly limited:
  - Users may update allowed profile fields.
  - Users may update notification read status for their own notifications.
  - Users must not be able to write ledger/balance/transactions directly.

### 3.6 Abuse mitigation (recommended baseline)
- Rate-limit transfer attempts in Cloud Functions (simple per-user time window).
- Firebase App Check recommended (enable once core flows work).

---

## 4) Final MVP Decisions (authoritative)
See `docs/decisions.md`. Summary:
- Auth: **Phone OTP**
- Recipient lookup: **Phone only (E.164)** — no email, no username, no QR
- Top-up: **Simulation**
- Transfers: Cloud Functions only, atomic + idempotent
- Balance: ledger-first + server-updated derived balance

---

## 5) Architecture Overview (Android + Firebase)

### 5.1 Android Client (Java)
**UI:** Material Design / Material Components  
**Pattern:** MVVM (recommended)
- UI (Activity/Fragment) observes ViewModel state
- ViewModel calls Repository
- Repository talks to Firebase (Auth/Firestore/Functions/FCM token mgmt)

**Local storage**
- Use Jetpack Security (EncryptedSharedPreferences) for:
  - app lock setting
  - inactivity timeout setting
  - notification preferences cache (optional)
- Room optional later for caching transaction lists, but Firestore offline cache may be enough for MVP.

**Navigation**
- Auth stack:
  - Splash → Welcome → Phone OTP → Profile Setup
- Main tabs:
  - Home (Dashboard)
  - Send
  - Transactions
  - Notifications
  - Settings
- Detail screens:
  - Transaction Detail (receipt)
  - Notification Preferences
  - Security settings (App lock/inactivity)

### 5.2 Firebase Backend
- **Auth:** Phone OTP
- **Firestore:** primary storage
- **Cloud Functions:** transaction engine, idempotency, notifications, audit logging
- **FCM:** push notifications
- **App Check:** reduce abuse (enable after baseline works)
- **Crashlytics/Analytics:** recommended later for observability

---

## 6) Data Model (Firestore) — MVP
### 6.1 Collections
**User profile**
- `users/{uid}`
  - `fullName` (string)
  - `phoneE164` (string, required)
  - `photoUrl` (string, optional)
  - `status` (string: `ACTIVE|FROZEN|DISABLED`)
  - `createdAt` (timestamp)
  - `lastLoginAt` (timestamp)
  - `updatedAt` (timestamp)

**Wallet**
- `wallets/{uid}`
  - `balance` (number; minor units recommended if you implement it, e.g., cents)
  - `currency` (string; for MVP can be fixed like `"USD"` or configured)
  - `updatedAt` (timestamp)
  - `limits` (object optional: dailySendLimit, perTxLimit)

**Transactions (global)**
- `transactions/{txId}`
  - `type` (`TRANSFER|TOPUP`)
  - `status` (`PENDING|COMPLETED|FAILED|REVERSED` (optional))
  - `senderUid` (string)
  - `receiverUid` (string; for TOPUP could be same as sender or null depending on design)
  - `amount` (number; minor units recommended)
  - `currency` (string)
  - `note` (string optional)
  - `referenceId` (string; safe display receipt id)
  - `idempotencyKey` (string)
  - `createdAt` (timestamp)
  - `completedAt` (timestamp optional)
  - `failedReason` (string optional)

**Ledger (per user)**
- `ledger/{uid}/entries/{entryId}`
  - `txId` (string)
  - `direction` (`debit|credit`)
  - `amount` (number)
  - `balanceAfter` (number)
  - `createdAt` (timestamp)

**Notifications (per user)**
- `notifications/{uid}/items/{notifId}`
  - `title` (string)
  - `body` (string)
  - `type` (`TRANSACTIONAL|SECURITY|ENGAGEMENT`)
  - `read` (boolean)
  - `deepLink` (string; app internal route)
  - `createdAt` (timestamp)

**Devices (per user)**
- `devices/{uid}/tokens/{tokenId}`
  - `fcmToken` (string)
  - `platform` (`android`)
  - `createdAt` (timestamp)
  - `lastSeenAt` (timestamp)

**Phone mapping (lookup + uniqueness)**
- `phones/{phoneE164}`
  - `uid` (string)
  - `createdAt` (timestamp)

**Audit logs**
- `auditLogs/{logId}`
  - `actorUid` (string)
  - `action` (string)
  - `targetUid` (string optional)
  - `txId` (string optional)
  - `metadata` (map/object)
  - `createdAt` (timestamp)

### 6.2 Normalization rules
- **Phone numbers must be stored as E.164** and used consistently:
  - `users/{uid}.phoneE164`
  - `phones/{phoneE164}` doc id

---

## 7) Firestore Security Rules — MVP intent (high-level)
The agent should implement rules consistent with:

### User data
- `users/{uid}`:
  - read: only if `request.auth.uid == uid`
  - write: only whitelisted fields (`fullName`, `photoUrl`, maybe `updatedAt`)
  - phoneE164 changes should be restricted and audited (for MVP: treat phone as immutable after onboarding)

### Wallet data
- `wallets/{uid}`:
  - read: only if `request.auth.uid == uid`
  - write: **never from client**

### Transactions
- `transactions/{txId}`:
  - read: user can read if senderUid==uid OR receiverUid==uid
  - write: **never from client** (created by Functions)

### Ledger
- `ledger/{uid}/entries/*`:
  - read: only own
  - write: **never from client**

### Notifications
- `notifications/{uid}/items/*`:
  - read: only own
  - write: allow updating `read` field only (or use Function to mark read)

### Devices tokens
- `devices/{uid}/tokens/*`:
  - write: user can create/update own tokens
  - read: optional (generally own only)

### Phones mapping
- `phones/{phoneE164}`:
  - write: only by server (Function) to prevent hijacking
  - read: recommended **deny client reads**; recipient resolution should happen server-side

---

## 8) Cloud Functions — Required behaviors (MVP)
Cloud Functions are the transaction engine.

### 8.1 createTransfer (callable or HTTPS)
**Input**
- `recipientPhoneE164` (string)
- `amount` (number; minor units recommended)
- `currency` (string)
- `note` (string optional)
- `idempotencyKey` (string required)

**Validations**
- Auth required
- Sender status ACTIVE
- Recipient exists and ACTIVE (resolved via `phones/{phoneE164}`)
- Not sending to self
- amount > 0
- sufficient balance
- enforce per-tx and daily limits if configured
- rate limit attempts

**Atomic effects**
- Create transaction record
- Update sender wallet balance (debit)
- Update receiver wallet balance (credit)
- Create ledger entries for both users
- Create notification records for sender/receiver
- Send FCM push to receiver and sender (transactional)

**Idempotency**
- If same sender + idempotencyKey already processed, return the existing `txId` and status.

### 8.2 simulateTopUp
**Input**
- `amount`, `currency`, `idempotencyKey`

**Effects**
- Create TOPUP transaction
- Credit wallet + ledger
- Notification + push

### 8.3 registerDeviceToken / token lifecycle
- On login / app start: create or update token in `devices/{uid}/tokens/*`
- On logout: disassociate token (delete doc or mark inactive)

### 8.4 Notification writing & deep links
- Every push should correspond to a Firestore notification inbox item.
- Deep link should open Transaction Detail for the tx.

---

## 9) Android Feature Implementation Notes (MVP)
### 9.1 Authentication (Phone OTP)
- Use Firebase phone auth
- After successful auth:
  - ensure `users/{uid}` exists
  - ensure `wallets/{uid}` exists (created by server bootstrap)
  - write phone mapping `phones/{phoneE164}` server-side

### 9.2 Send money UX
- Recipient search box: accept phone input
- Normalize to E.164 before calling transfer
- Show:
  - recipient confirmation (masked phone)
  - amount entry
  - review screen
  - progress screen (disable repeated submits)
  - result receipt with `referenceId`

### 9.3 Transaction list & receipt
- Paginated list
- Detail screen shows:
  - status, timestamps
  - sender/receiver (masked)
  - amount, referenceId
  - “Share receipt” (share text)

### 9.4 Notifications inbox
- Firestore listener or paging query to `notifications/{uid}/items`
- Read/unread state
- Deep link handling for transaction detail

---

## 10) Development Order (strong recommendation)
1. Implement Firestore schema + security rules skeleton
2. Implement Cloud Functions: createTransfer + idempotency + atomic ledger/balance updates
3. Implement Android Auth + Profile setup
4. Implement Home (balance) + Transactions list
5. Implement Send flow wired to `createTransfer`
6. Implement Notifications (push + inbox)
7. Harden: App Check, rate limits, Crashlytics, app lock

---

## 11) Documentation Structure
- `docs/decisions.md` — locked MVP decisions
- `docs/features/` — feature-by-feature specs (contracts, screens, rules)

Start with: `docs/features/README.md`