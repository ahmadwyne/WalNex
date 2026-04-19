# Feature: Notifications (Push + In-App) (FR-19..FR-25)

## Goals
- Transactional notifications (sent/received/top-up/failures)
- Security notifications (new device sign-in, profile changes) - baseline support
- In-app inbox with read/unread and deep links

## Data model
- `notifications/{uid}/items/{notifId}`: inbox items
- `devices/{uid}/tokens/{tokenId}`: FCM tokens

## Flows
- On login/app start:
  - register/update token in Firestore
- On logout:
  - delete/disassociate token
- On event (transfer/top-up):
  - write notification item to Firestore
  - send FCM push
  - deep link to transaction detail

## Preferences (FR-22)
- For MVP: store preferences (on/off for engagement) in `users/{uid}` or a dedicated doc.
- Transactional notifications should generally remain enabled (policy decision).

## Edge cases
- Token refresh: update Firestore token record.
- Multiple devices: store multiple tokens per user.