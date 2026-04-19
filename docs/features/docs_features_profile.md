# Feature: User Profile (FR-6..FR-7)

## Goal
Allow user to set up and edit basic profile fields.

## Screens
- Profile Setup (first run)
- Edit Profile

## Fields (minimum)
Stored in `users/{uid}`:
- `fullName` (required for MVP)
- `phoneE164` (required; from Auth)
- `photoUrl` (optional)
- `status` (ACTIVE/FROZEN/DISABLED)

## Rules
- Phone number should be treated as immutable for MVP (changes require support flow later).
- Only allow client to update whitelisted fields (e.g., `fullName`, `photoUrl`).

## Audit (FR-7)
Create an audit log entry for:
- profile edits (at least for `fullName`/photo changes in MVP)
- any status change (server/admin only)

Audit entries go to `auditLogs/{logId}`.