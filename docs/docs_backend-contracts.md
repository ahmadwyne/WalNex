# Walnex — Backend Contracts (Cloud Functions + Firestore) (MVP)

**Applies to:** MVP build based on PRD dated **2026-04-19**  
**Auth:** Firebase Phone OTP  
**Recipient lookup:** **Phone only (E.164)**  
**Money movement:** Cloud Functions only (atomic + idempotent)  
**Top-up:** Simulation (FR-17A)

This document defines **exact request/response contracts**, error codes, and core backend behaviors so the Android client can implement flows without ambiguity.

---

## 1) Conventions (must follow)

### 1.1 Currency & amounts
- **MVP recommendation:** store and transmit amounts in **minor units** (integer), e.g. cents.
  - Example: $10.50 → `amountMinor = 1050`
- If you choose to use decimals, you must avoid floating point errors; minor-units is strongly recommended.

In this doc, we use:
- `amountMinor` (integer)
- `currency` (string, e.g. `"USD"`)

### 1.2 Phone normalization
- All phones must be **E.164**.
- The **server** validates phone format; the client should normalize using a phone library (recommended) before calling.

### 1.3 Idempotency
- Every money-movement call requires `idempotencyKey` (string).
- **Definition:** For a given `senderUid`, the same `idempotencyKey` must return the same operation result (same `txId`, same status) and never double-debit.

### 1.4 Transaction statuses
- `PENDING` (optional transient state)
- `COMPLETED`
- `FAILED`
- `REVERSED` (reserved for future admin reversal flow)

For MVP, recommended states are `COMPLETED` and `FAILED` (you can still create as `PENDING` then finalize in the same function).

### 1.5 Error object shape (standard)
All functions return errors in a consistent structure.

**Error object**
```json
{
  "ok": false,
  "error": {
    "code": "SOME_CODE",
    "message": "Human-readable summary safe to show to user.",
    "details": {
      "field": "optional",
      "debug": "optional"
    }
  }
}
```

**Success object**
```json
{
  "ok": true,
  "data": { }
}
```

> If using Firebase callable functions, you may map these to `HttpsError`. If you do, still ensure the client can obtain `code` and `message` reliably.

---

## 2) Firestore Contracts (server-owned vs client-owned)

### 2.1 Server-owned (client must never write)
- `wallets/{uid}`
- `ledger/{uid}/entries/{entryId}`
- `transactions/{txId}` (except optional “request” collection if you add one later)
- `phones/{phoneE164}` (must be server-written to prevent hijacking)

### 2.2 Client-writable (restricted)
- `users/{uid}` (only whitelisted profile fields like `fullName`, `photoUrl`)
- `devices/{uid}/tokens/{tokenId}` (own token docs only)
- `notifications/{uid}/items/{notifId}` (only `read` toggles, if allowed)

---

## 3) Function: `createTransfer` (Core P2P)

### 3.1 Purpose
Transfers money from authenticated sender to recipient identified by **phoneE164**.

### 3.2 Invocation
- Callable function name: `createTransfer`
- Auth required: **Yes** (`context.auth.uid` must exist)

### 3.3 Request (exact)
```json
{
  "idempotencyKey": "string-required",
  "recipientPhoneE164": "+15551234567",
  "amountMinor": 1500,
  "currency": "USD",
  "note": "optional note"
}
```

**Field rules**
- `idempotencyKey`: required; length 8–128 recommended
- `recipientPhoneE164`: required; must be valid E.164
- `amountMinor`: required; integer > 0
- `currency`: required; for MVP you may enforce a single currency
- `note`: optional; length 0–140 recommended

### 3.4 Success response (exact)
```json
{
  "ok": true,
  "data": {
    "txId": "string",
    "referenceId": "WX-2026-00001234",
    "status": "COMPLETED",
    "senderUid": "string",
    "receiverUid": "string",
    "amountMinor": 1500,
    "currency": "USD",
    "createdAt": 1760000000000,
    "completedAt": 1760000000500
  }
}
```

**Notes**
- `createdAt` / `completedAt` are epoch millis for easy client handling (you may also store Firestore Timestamps in DB; this is just response format).
- If idempotency triggers, return the same `txId` and current/final status.

### 3.5 Idempotency behavior (required)
If the same sender calls again with the same `idempotencyKey`:
- Return the **same** success payload (same `txId`) if previously completed
- Or return a failure payload if previously failed (same `txId` if recorded)
- Never create additional ledger entries or balance updates

**Recommended implementation (one approach)**
- `idempotency/{senderUid}/keys/{idempotencyKey}` storing:
  - `txId`, `status`, `createdAt`, `locked`/`processing`
- Ensure atomic creation/check inside the same transaction.

### 3.6 Failure response (exact)
```json
{
  "ok": false,
  "error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "You don’t have enough balance to send this amount.",
    "details": {
      "availableMinor": 1200,
      "requestedMinor": 1500
    }
  }
}
```

### 3.7 Error codes (canonical list)
The Android client should handle these explicitly:

**Auth / permissions**
- `UNAUTHENTICATED` — not logged in
- `PERMISSION_DENIED` — account status not allowed or blocked

**Validation**
- `INVALID_ARGUMENT` — missing/invalid request fields
- `INVALID_PHONE` — recipientPhoneE164 invalid format
- `INVALID_AMOUNT` — amountMinor <= 0 or exceeds max
- `INVALID_CURRENCY` — unsupported currency

**Recipient resolution**
- `RECIPIENT_NOT_FOUND` — phone mapping not found
- `RECIPIENT_INACTIVE` — recipient is FROZEN/DISABLED
- `SENDER_INACTIVE` — sender is FROZEN/DISABLED
- `SELF_TRANSFER_NOT_ALLOWED` — sender and recipient same uid

**Business rules**
- `INSUFFICIENT_FUNDS`
- `LIMIT_EXCEEDED` — per-tx or daily limit exceeded
- `RATE_LIMITED` — too many attempts in time window

**Idempotency / concurrency**
- `IDEMPOTENCY_KEY_REUSED_DIFFERENT_PAYLOAD` — same key but different fields (optional but recommended)
- `ALREADY_PROCESSING` — idempotency key locked by an in-flight request (optional)

**Internal**
- `INTERNAL` — unexpected server error
- `FAILED_PRECONDITION` — data integrity issue (e.g., wallet doc missing)

### 3.8 Backend side effects (must occur on success)
On successful transfer completion, the function must:

1) Create `transactions/{txId}`:
- `type = "TRANSFER"`
- `status = "COMPLETED"`
- `senderUid`, `receiverUid`
- `amountMinor`, `currency`
- `note` (optional)
- `referenceId` (display-safe receipt id)
- `idempotencyKey`
- timestamps

2) Update balances atomically:
- decrement `wallets/{senderUid}.balanceMinor`
- increment `wallets/{receiverUid}.balanceMinor`

3) Create ledger entries:
- `ledger/{senderUid}/entries/{entryId}`:
  - `direction = "debit"`
  - `amountMinor`
  - `balanceAfterMinor`
- `ledger/{receiverUid}/entries/{entryId}`:
  - `direction = "credit"`
  - `amountMinor`
  - `balanceAfterMinor`

4) Create in-app notification items:
- `notifications/{senderUid}/items/{notifId}`: "Transfer sent"
- `notifications/{receiverUid}/items/{notifId}`: "Money received"
Each includes `deepLink` to transaction detail screen, e.g. `walnex://tx/<txId>`.

5) Send FCM push notifications
- To receiver (and optionally sender)

### 3.9 Suggested deep link contract
- Transaction detail deep link: `walnex://tx/<txId>`

---

## 4) Function: `simulateTopUp` (MVP Add Money)

### 4.1 Purpose
Credits the authenticated user’s wallet with simulated funds for demo/testing.

### 4.2 Request (exact)
```json
{
  "idempotencyKey": "string-required",
  "amountMinor": 5000,
  "currency": "USD",
  "source": "SIMULATED"
}
```

### 4.3 Success response (exact)
```json
{
  "ok": true,
  "data": {
    "txId": "string",
    "referenceId": "WX-2026-00004567",
    "status": "COMPLETED",
    "uid": "string",
    "amountMinor": 5000,
    "currency": "USD",
    "createdAt": 1760000000000,
    "completedAt": 1760000000200
  }
}
```

### 4.4 Error codes
- `UNAUTHENTICATED`
- `INVALID_ARGUMENT`
- `INVALID_AMOUNT`
- `LIMIT_EXCEEDED` (top-up limit)
- `RATE_LIMITED`
- `INTERNAL`

### 4.5 Side effects
- Create `transactions/{txId}` with `type="TOPUP"`, `status="COMPLETED"`
- Credit `wallets/{uid}`
- Create one ledger entry (credit)
- Create notification item + FCM push

---

## 5) Function: `registerDeviceToken` (FCM token lifecycle)

### 5.1 Purpose
Registers/updates the device’s FCM token for the signed-in user.

### 5.2 Request (exact)
```json
{
  "fcmToken": "string-required",
  "platform": "android",
  "deviceId": "string-optional"
}
```

### 5.3 Success response (exact)
```json
{
  "ok": true,
  "data": {
    "tokenId": "string",
    "updated": true
  }
}
```

### 5.4 Notes
- Token docs stored in `devices/{uid}/tokens/{tokenId}`
- `tokenId` can be a hash of `fcmToken` (recommended) to avoid duplicates.

---

## 6) Function: `logoutDeviceToken` (optional but recommended)
If you want explicit cleanup on logout.

### Request
```json
{
  "fcmToken": "string-required"
}
```

### Response
```json
{
  "ok": true,
  "data": { "deleted": true }
}
```

---

## 7) Backend bootstrap (recommended)
To ensure consistent setup, implement one of:
- Auth trigger (on user create) initializes `users/{uid}`, `wallets/{uid}`, and `phones/{phoneE164}`
**or**
- A callable `bootstrapUser()` invoked by the client after auth.

### Suggested bootstrap behavior
- Create/merge `users/{uid}` with `status="ACTIVE"`
- Create `wallets/{uid}` with `balanceMinor=0`
- Create `phones/{phoneE164}` mapping if not exists:
  - If exists and points to different uid → return error `PHONE_ALREADY_IN_USE`

---

## 8) Receipt / referenceId format
`referenceId` must be:
- unique
- safe to share
- stable for the transaction

Example formats:
- `WX-2026-00001234` (sequence requires counter logic)
- `WX-<shortTxId>` (derived from txId)

For MVP, easiest is:
- `referenceId = "WX-" + txId.substring(0, 8).toUpperCase()`

---

## 9) Client handling requirements (Android)
The Android client must:
- Generate and persist `idempotencyKey` per transfer attempt until a terminal response is received
- Prevent double-submit in UI
- On timeout/network error: allow retry using the same `idempotencyKey`
- Display errors based on `error.code`:
  - `INSUFFICIENT_FUNDS` → show available
  - `RECIPIENT_NOT_FOUND` → “No Walnex user found for this phone”
  - `RATE_LIMITED` → “Too many attempts, try again later”

---

## 10) Testing checklist (backend)
Minimum backend tests (manual or automated):
- createTransfer success path updates both balances and ledger entries correctly
- idempotency: same key returns same txId and does not double debit
- insufficient funds rejects and does not create partial records
- recipient not found rejects
- self-transfer rejected
- frozen sender/recipient rejected
- simulated top-up idempotency works

---
**End of document**