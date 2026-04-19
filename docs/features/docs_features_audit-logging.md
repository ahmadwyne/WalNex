# Feature: Audit Logging (NFR + FR-7 direction)

## Goal
Record sensitive actions for investigation and compliance.

## Minimum events to log (MVP)
- transfer created/completed/failed (actorUid, txId, recipient)
- top-up created/completed/failed
- profile edits
- device token changes (optional)
- account status changes (server/admin only)

## Storage
- `auditLogs/{logId}` (global collection)

Suggested fields:
- `actorUid`
- `action`
- `targetUid` (optional)
- `txId` (optional)
- `metadata` (object)
- `createdAt`

## Access
- End users: no access
- Admin/support: future enhancement via custom claims + audited reads