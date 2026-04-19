# Feature: Wallet, Balance & Ledger (FR-8..FR-11)

## Goal
Show the user a trusted balance and a ledger-backed history.

## Data model
- `wallets/{uid}`:
  - `balance`
  - `currency`
  - `updatedAt`
- `ledger/{uid}/entries/{entryId}`:
  - `txId`
  - `direction` (debit/credit)
  - `amount`
  - `balanceAfter`
  - `createdAt`

## Invariants
- Client cannot write wallet balances.
- Every money movement produces ledger entry/entries.
- Balance changes must be atomic with ledger writes.

## UI
- Home dashboard:
  - current balance
  - recent transactions (subset)
- Optional: show "last updated" timestamp.

## Implementation notes
- Prefer minor units (cents) to avoid floating point issues.
- Paginate ledger/transaction history to avoid heavy reads.