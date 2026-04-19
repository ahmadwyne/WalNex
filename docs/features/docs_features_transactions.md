# Feature: Transactions & Receipts (FR-10..FR-11)

## Goal
Provide a clear transaction history and receipt details.

## Screens
- Transactions list (paginated)
- Filters:
  - date range
  - type (TRANSFER/TOPUP)
  - status (PENDING/COMPLETED/FAILED)
- Transaction detail (receipt)
  - referenceId
  - status, timestamps
  - sender/receiver (masked)
  - share receipt

## Data source
- `transactions/{txId}` for the canonical record
- optionally show ledger entries for running balance after each tx

## Indexing notes
Plan indexes for querying a user's transactions efficiently (e.g., by createdAt + participant).