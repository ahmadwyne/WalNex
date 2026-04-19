# Walnex (E‑Wallet) — Requirements Document (PRD)

**Product:** Walnex (E‑Wallet)  
**Platform:** Android (native)  
**Client language:** Java  
**Backend/Database:** Firebase (Auth + Firestore + Cloud Functions)  
**Notifications:** Firebase Cloud Messaging (FCM) + in‑app inbox  
**Document version:** 1.0  
**Date:** 2026-04-19

---

## 1) Purpose
Walnex is a mobile wallet application that enables authenticated users to manage a wallet profile, view balance and transaction history, send money to other Walnex users, add money (simulated for MVP), and receive notifications (push + in‑app). The system must enforce strong security and integrity: **all money movement is executed server-side**.

---

## 2) Goals & Success Criteria
### 2.1 Product Goals
- Provide a simple, reliable wallet experience for everyday P2P transfers.
- Provide clear receipts and history to build user trust.
- Provide robust notification handling for transactional and security events.
- Enforce wallet-grade security: server-side money movement, least-privilege data access, audit logging.

### 2.2 MVP Success Criteria (measurable)
- Users can successfully onboard with Phone OTP and land on Home.
- Users can send money to another existing Walnex user and both see it reflected in history.
- Balance and ledger remain consistent (no double debit, no partial updates).
- Notifications (push + in-app) are generated for transfers and top-ups.
- App remains stable (no crash loops) and handles invalid inputs gracefully.

---

## 3) Scope
### 3.1 In Scope
- User onboarding and authentication.
- Wallet profile setup (basic identity fields).
- Wallet balance display and transaction history.
- P2P transfers between Walnex users.
- “Add money” (top-up) simulation or real payment integration (per phase).
- Notifications:
  - Transactional
  - Security
  - Engagement (optional, user-controlled)
- Admin/support tooling (minimal; optional but recommended).
- Audit logs for sensitive actions.

### 3.2 Out of Scope (for MVP unless explicitly added)
- Withdraw/cash-out (requires compliance).
- Full admin dashboard UI (prefer separate admin app/dashboard).
- KYC/AML flows.

---

## 4) Personas
### 4.1 End User
- Sends/receives money
- Checks balance and transaction history
- Manages notification preferences
- Uses optional app lock features

### 4.2 Support/Admin (Optional)
- Reviews user status
- Investigates failed transactions
- Freezes/unfreezes accounts (policy controlled)
- Accesses audit logs (restricted)

---

## 5) Assumptions & Decisions (MVP)
> The PRD allows multiple choices; the MVP must lock decisions. If different later, update this section and `docs/decisions.md`.

**MVP decisions (current):**
- Authentication: **Phone OTP**
- Recipient lookup: **Phone-only search (E.164)** *(no email/username/QR in MVP)*
- Top-up: **Simulation** (FR-17A)
- Money movement: **Cloud Functions only**, atomic + idempotent
- Balance: ledger-first + server-updated balance

---

## 6) System Overview (Proposed Architecture)
### 6.1 Client (Android, Java)
- UI: Material Design (Material Components)
- Architecture: MVVM (ViewModel + Repository)
- Local storage:
  - EncryptedSharedPreferences / Jetpack Security for tokens/settings
  - Room optional for caching

### 6.2 Firebase Components
- Firebase Authentication:
  - Phone OTP (MVP)
  - (Optional future) Email/Password, Google Sign-In
- Cloud Firestore:
  - primary database for profiles, wallets, transactions, ledger, notifications
- Cloud Functions:
  - executes transaction logic server-side (transfers/top-ups)
  - generates ledger entries
  - sends notifications
- Firebase Cloud Messaging (FCM):
  - push notifications
- Firebase App Check:
  - reduce abuse from non-genuine clients (recommended)
- Cloud Storage (optional):
  - avatars/documents (if needed)
- Crashlytics + Analytics:
  - reliability and product insights (recommended)

---

## 7) Functional Requirements (FR)

### 7.1 Authentication & Account
- **FR-1** User shall be able to register using selected method(s).
  - MVP: Phone OTP
- **FR-2** User shall be able to sign in and sign out.
- **FR-3** User session shall persist securely across app restarts.
- **FR-4** User shall be able to reset credentials.
  - Phone: re-verify phone (flow definition required)
  - (If email later) password reset
- **FR-5** System shall lock down access to wallet data using Auth + Firestore Security Rules (no public reads).

### 7.2 User Profile
- **FR-6** User shall be able to create and edit profile fields (minimum):
  - Full name
  - Phone (from Auth; typically immutable post-onboarding in MVP)
  - Profile picture (optional)
  - Default currency (if applicable)
- **FR-7** System shall store profile change history or at least audit entries for sensitive edits (e.g., phone change).

### 7.3 Wallet & Balance
- **FR-8** User shall be able to view current wallet balance.
- **FR-9** System shall compute balance from a ledger or maintain a balance field with strict server-side updates.
  - Recommendation: ledger-first + derived balance, or atomic updates in server functions.
- **FR-10** User shall be able to view transaction history with filters (date range, type, status).
- **FR-11** Each transaction shall have:
  - unique ID
  - timestamps
  - amount
  - currency
  - sender
  - receiver
  - type
  - status
  - reference/meta

### 7.4 P2P Transfers
- **FR-12** User shall be able to send money to another Walnex user by:
  - MVP: **phone lookup only** (E.164)
- **FR-13** System shall validate transfer rules before executing:
  - sufficient balance
  - not sending to self
  - amount limits
  - account status
- **FR-14** Transfers shall be executed server-side (Cloud Functions) to prevent client tampering.
- **FR-15** Transfer execution shall be atomic:
  - debit sender
  - credit receiver
  - generate ledger entries
  - update balances
  - create notification events
- **FR-16** System shall provide clear transfer states:
  - Pending / Completed / Failed / Reversed

### 7.5 Top-up / Add Money
- **FR-17A (MVP)** User shall be able to add money via simulated top-up for demo/testing with admin-configured limits.
- **FR-17B (Phase 2)** User shall be able to top-up via a payment provider with webhook verification and reconciliation.

### 7.6 Withdraw / Cash-out (Optional)
- **FR-18** If enabled, user shall be able to request withdrawal to a bank/mobile money account.

### 7.7 Notifications System (Push + In-App)
- **FR-19** System shall send transactional notifications (FCM) for:
  - money received
  - money sent
  - transfer failed
  - top-up success/failure
- **FR-20** System shall send security notifications for:
  - new device sign-in
  - password reset / re-verification events
  - profile changes
  - suspicious activity flags
- **FR-21** System shall support engagement notifications (optional):
  - reminders, promotions, announcements (opt-in recommended)
- **FR-22** User shall be able to manage notification preferences:
  - enable/disable categories (policy: transactional/security may be mandatory)
  - quiet hours (optional)
  - sound/vibration controls (as permitted by OS)
- **FR-23** System shall store notifications in an in-app notification center with read/unread status.
- **FR-24** Notifications shall deep-link to relevant screens (transaction detail, security page).
- **FR-25** System shall support device token registration and lifecycle:
  - create/update FCM token on login
  - delete/disassociate on logout

### 7.8 Security & Device Controls
- **FR-26** App shall support biometric/PIN lock for opening the app (optional but recommended).
- **FR-27** App shall auto-logout or require re-authentication after configurable inactivity time.
- **FR-28** System shall detect and restrict usage on rooted devices (policy decision; at least warn users).

### 7.9 Admin / Support (Recommended)
- **FR-29** Admin shall be able to search for users and view wallet/transaction history (read-only).
- **FR-30** Admin shall be able to flag accounts (freeze/unfreeze) and optionally trigger manual reversals (with audit trail).

---

## 8) Data Requirements (Firestore Model — High Level)
### 8.1 Collections (suggested)
- `users/{uid}`: profile, status, createdAt, lastLoginAt
- `wallets/{uid}`: balance, currency, limits, updatedAt
- `transactions/{txId}`: senderUid, receiverUid, amount, status, type, createdAt, completedAt
- `ledger/{uid}/entries/{entryId}`: txId, direction (debit/credit), amount, balanceAfter, createdAt
- `notifications/{uid}/items/{notifId}`: title, body, type, read, createdAt, deepLink
- `devices/{uid}/tokens/{tokenId}`: fcmToken, platform, createdAt, lastSeenAt

### 8.2 Data integrity rules
- Wallet balance and ledger writes must be performed only by Cloud Functions / privileged service accounts.
- Clients may read own data.
- Clients may create “requests” only if using request-driven flow (optional design).

---

## 9) Non‑Functional Requirements (NFR)

### 9.1 Security
- All sensitive operations executed server-side.
- Firestore Security Rules: least privilege per-user constraints.
- Encrypt sensitive local data at rest (Jetpack Security).
- Use Firebase App Check to mitigate abuse.
- Rate limit transaction attempts (Cloud Functions + counters/time windows).
- Comprehensive audit logging for transactions and account status changes.

### 9.2 Privacy & compliance
- Provide privacy policy and data deletion request path.
- Log retention policy (transactions retained per legal/business requirements).
- Minimize stored PII; protect with access controls.
- Follow notification consent best practices and local regulations.

### 9.3 Reliability & availability
- Cloud Functions should be idempotent (avoid duplicate debits/credits).
- Retry-safe design: client can re-submit with same idempotency key.
- Graceful error handling and clear user messaging.

### 9.4 Performance
- Fast first meaningful paint.
- Paginate transaction lists.
- Plan Firestore indexes for transaction queries (uid, createdAt, status).

### 9.5 Observability
- Crashlytics for crashes; Analytics optional for funnels.
- Server logs for transaction execution and failures.
- Alerts on error rate spikes (GCP Monitoring).

### 9.6 Maintainability
- Clean architecture boundaries (UI, domain, data).
- Consistent naming conventions, code style, and documentation.

---

## 10) UX / UI Requirements
- Material Design compliant UI.
- Clear balance visibility and transaction status.
- Confirmation screens for transfers (amount, receiver, fees if any).
- Provide receipts (transaction detail with reference ID).
- Accessible design:
  - contrast
  - scalable text
  - TalkBack labels

> Detailed visual system is documented in `docs/ui/README.md`.

---

## 11) App Flow (End-to-End)

### 11.1 First-time User Flow (Onboarding)
1. Splash / App start
2. Welcome (“Create account” / “Sign in”)
3. Auth (Phone OTP)
4. Basic Profile Setup (Name, optional photo)
5. Security Setup (optional): biometric/PIN lock
6. Contextual permissions prompts:
   - explain value then request notification permission
7. Wallet created → land on Home (Dashboard)

### 11.2 Returning User Flow
1. Splash
2. (If enabled) App Lock (biometric/PIN)
3. Home (Dashboard)

### 11.3 Core Wallet Navigation (Primary Screens)
- Home (Dashboard): balance, quick actions, recent transactions
- Send / Transfer: recipient → amount → review → confirm → status
- Transactions: full history + filters → transaction detail
- Notifications: inbox + deep links
- Profile/Settings: profile, security, notification prefs, help, logout

### 11.4 P2P Transfer Flow (Industry-standard)
1. Home → Send
2. Select recipient:
   - MVP: search by phone
3. Enter amount:
   - show available balance and limits
4. Review:
   - recipient (masked), amount, optional note, reference placeholder
5. Confirm:
   - optional biometric/PIN confirmation
6. Processing:
   - show progress + prevent double-submit
7. Result:
   - Success: receipt + share receipt
   - Fail: reason + retry guidance
8. Notifications:
   - Sender: “Transfer sent”
   - Receiver: “Money received”

### 11.5 Add Money / Top-up Flow
- Simulated Top-up (MVP):
  - Add Money → choose amount → confirm → success + receipt
- Real payments (Phase 2):
  - Add Money → payment provider → webhook verify → wallet credit → receipt

### 11.6 Notifications Flow
Backend event → write notification record → send push → user taps → deep link → mark read.

---

## 12) User Stories (with Acceptance Criteria)

### Epic A — Authentication & Account
**US-A1** As a user, I want to sign up using my phone so I can access Walnex.  
Acceptance:
- Given valid OTP, when I verify, my account is created and I land on Home.
- Invalid OTP shows a clear error without crashing.

**US-A2** As a user, I want to log out so I can protect my wallet on shared devices.  
Acceptance:
- Logout clears local session and disassociates device token (or marks inactive).

### Epic B — Wallet Dashboard & Transactions
**US-B1** As a user, I want to see my current balance so I know how much I can spend/send.  
Acceptance:
- Balance loads within acceptable time; if offline, show last cached with “last updated”.

**US-B2** As a user, I want to see my transaction history so I can track money movement.  
Acceptance:
- List is paginated; selecting an item shows details with reference ID, status, timestamps.

### Epic C — Send Money (P2P)
**US-C1** As a user, I want to send money to another Walnex user so I can pay them instantly.  
Acceptance:
- If amount ≤ balance and recipient exists, transfer completes and both see it in history.
- If insufficient funds, transfer is rejected with a clear message.
- Double-tap confirm cannot create two transfers (idempotency enforced).

**US-C2** As a user, I want a receipt/reference for each transfer so I can prove payment.  
Acceptance:
- Transaction detail includes unique reference ID and share option.

### Epic D — Notifications
**US-D1** As a user, I want a push notification when I receive money so I don’t miss incoming payments.  
Acceptance:
- Receiver gets push quickly; it deep-links to the transaction.

**US-D2** As a user, I want to control notification preferences so I only receive what I want.  
Acceptance:
- I can toggle categories; policy enforced for mandatory transactional/security.

**US-D3** As a user, I want an in-app notification inbox so I can review past alerts.  
Acceptance:
- Notifications are stored with read/unread; can mark all as read.

### Epic E — Security
**US-E1** As a user, I want biometric/PIN lock so my wallet is protected if my phone is unlocked.  
Acceptance:
- If enabled, app requires biometric/PIN on open (and optionally on sensitive actions).

**US-E2** As a user, I want security notifications so I can react quickly.  
Acceptance:
- New device login / credential reset triggers a security notification.

### Epic F — Admin/Support (if included)
**US-F1** As support, I want to search users and view transactions so I can investigate issues.  
Acceptance:
- Support can view but not change balances directly.
- All admin actions are audited.

---

## 13) Roles & Permissions (RBAC)

### 13.1 Roles
1. USER (End User)
2. SUPPORT (Support Agent)
3. FINANCE_OP (optional)
4. ADMIN
5. SYSTEM (Cloud Functions/service account)

### 13.2 Permission Matrix (High Level)
| Capability | USER | SUPPORT | FINANCE_OP | ADMIN | SYSTEM |
|---|---|---|---|---|---|
| Register/Login | Yes | No | No | No | N/A |
| View own profile | Yes | Limited read | Limited read | Yes | N/A |
| Edit own profile | Yes | No | No | No | N/A |
| View own wallet/balance | Yes | Read-only | Read-only | Yes | Yes |
| Send money (initiate) | Yes | No | No | No | Executes |
| Create/modify balances directly | No | No | No | No | Yes (controlled) |
| View other users’ transactions | No | Yes (scoped) | Yes (broader) | Yes | Yes |
| Freeze/unfreeze account | No | Maybe | Yes/No | Yes | Yes |
| Reverse/refund transaction | No | No | Yes (approvals) | Yes | Yes |
| Manage notification campaigns | No | No | No | Yes | Yes |
| Access audit logs | No | Limited | Yes | Yes | Yes |

### 13.3 Enforcement Requirements
- All money movement executed by SYSTEM only.
- Clients can only request a transfer; server validates + commits.
- Support access minimal and logged (who accessed what, when, why).
- Least privilege in rules and function authorization.

---

## 14) Roles-to-Data Access Rules (Firestore Guidance)

### 14.1 Data Ownership (User)
- `users/{uid}`: user reads own; writes limited fields
- `wallets/{uid}`: user reads own; no direct writes
- `transactions`: user reads where senderUid==uid OR receiverUid==uid
- `ledger/{uid}/entries`: user reads own; no writes
- `notifications/{uid}/items`: user reads own; may update read-status only

### 14.2 Admin/Support Access
- Prefer a separate admin dashboard using privileged backend APIs.
- If support/admin reads Firestore directly:
  - require custom claims
  - strict rules
  - logging of access (proxy API preferred)

---

## 15) Navigation Map (Simple)
- Auth stack:
  - Welcome → Login (OTP) → Profile Setup
- Main tabs:
  - Home
  - Send
  - Transactions
  - Notifications
  - Settings
- Detail screens:
  - Transaction Detail (Receipt)
  - Notification Preferences
  - Security (App Lock, Devices)

---

## 16) References (internal docs)
- UI Design System: `docs/ui/README.md`
- Backend Contracts: `docs/backend-contracts.md`
- MVP Decisions: `docs/decisions.md`
- Feature specs index: `docs/features/README.md`