# Feature: P2P Transfers (Phone-only) (FR-12..FR-16)

## Goal
Allow a user to send money to another Walnex user using **phone number lookup only**.

## Recipient lookup (MVP)
- Phone number only, normalized to **E.164** (e.g., `+15551234567`).

## Screens
- Send: Recipient phone entry/search
- Send: Amount entry
- Send: Review (recipient, amount, optional note)
- Send: Processing (disable confirm button; show progress)
- Result: Success/Fail + Receipt (Transaction detail)

## Firestore supporting lookup mapping
- `phones/{phoneE164}` → `{ uid }` (server-written)

## Cloud Function: createTransfer
### Input
- `recipientPhoneE164` (required)
- `amount` (required)
- `currency` (required)
- `note` (optional)
- `idempotencyKey` (required)

### Validations
- caller is authenticated
- sender status ACTIVE
- recipient exists and ACTIVE
- not sending to self
- amount > 0
- sufficient funds
- rate limiting / attempt limiting (recommended)

### Atomic effects (server-side)
- create `transactions/{txId}` (type TRANSFER)
- update sender wallet balance (debit)
- update receiver wallet balance (credit)
- write ledger entries for both users
- write in-app notification items for both users
- send FCM notifications (transactional)

### Status model
- Pending / Completed / Failed (Reversed optional later)

### Edge cases
- Double-tap confirm must not create two transfers (idempotency).
- Client retries after timeout must be safe (idempotency).
- Recipient phone normalization must be consistent across auth/profile/lookup.