# Feature: Security & Device Controls (FR-26..FR-28)

## Goal
Add wallet-grade protection controls.

## MVP recommendations
- App lock (biometric/PIN) for opening the app (optional, user-controlled)
- Inactivity timeout re-auth (optional)
- Root detection policy:
  - at least warn users in MVP

## Notes
- Keep UX clear and avoid blocking legitimate users unless required.
- Sensitive actions (send money) may require extra confirmation (optional policy).