# Feature: Authentication & Account (FR-1..FR-5)

## Goal
Allow users to register/sign in using **Phone OTP** and maintain a secure session.

## Screens
- Splash
- Welcome (Create account / Sign in)
- Phone entry + OTP request
- OTP verification
- Profile setup (first login)
- Logout confirmation

## Firebase
- Firebase Auth Phone provider

## Firestore responsibilities after login
After first successful auth:
- Ensure `users/{uid}` exists (create if missing).
- Ensure `wallets/{uid}` exists (create if missing).
- Ensure `phones/{phoneE164}` mapping exists and points to this `uid`.

**Important:** `phones/{phoneE164}` must be written server-side to prevent phone hijacking.

## Session persistence (FR-3)
- Firebase Auth persists session by default.
- Store minimal local settings using EncryptedSharedPreferences (no wallet balances).

## Security notes (FR-5)
- Firestore rules must enforce:
  - no public reads
  - user reads only their own docs
  - wallet/ledger writes only by server

## Edge cases
- User deletes app and reinstalls: should log in again via OTP and regain access.
- `users/{uid}` exists but `wallets/{uid}` missing: backend bootstrap should repair.
- Duplicate phone number mapping: must be prevented via transaction/uniqueness enforcement.