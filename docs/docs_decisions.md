# Walnex — MVP Decisions

**Date:** 2026-04-19

## Authentication
- **Phone OTP** (Firebase Auth Phone)

## Recipient lookup (Send Money)
- **Phone number only** (E.164 normalized).
- No email lookup.
- No username lookup.
- No QR scanning.

## Top-up
- **Simulated top-up** for MVP (FR-17A).

## Money integrity
- Cloud Functions execute all money movement.
- Ledger-first + server-updated balances.
- Idempotency required for transfer/top-up.