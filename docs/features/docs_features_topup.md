# Feature: Add Money / Top-up (Simulation MVP) (FR-17A)

## Goal
Allow user to add money via a simulated top-up for demo/testing.

## Screens
- Add Money
- Enter amount
- Confirm
- Result + Receipt

## Cloud Function: simulateTopUp
### Input
- `amount`
- `currency`
- `idempotencyKey`

### Behavior
- validate amount within allowed limits
- create transaction type TOPUP
- credit wallet + write ledger entry
- notify user (push + in-app)

## Notes
Real payment integrations (FR-17B) are deferred to Phase 2.