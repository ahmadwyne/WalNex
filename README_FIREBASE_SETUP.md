# Firebase Setup Guide (WalNex Auth + Account)

This guide configures Firebase for the current Android app implementation.

## 1) Prerequisites

- A Firebase project (Blaze plan is recommended for phone auth and functions).
- Firebase CLI installed:
  - `npm install -g firebase-tools`
- Logged in:
  - `firebase login`
- Project root is this folder (contains `build.gradle.kts`).

## 2) Register Android App in Firebase

1. Open Firebase Console > Project Settings > General.
2. Add Android app with package name:
   - `com.example.walnex`
3. Download `google-services.json`.
4. Place it at:
   - `app/google-services.json`

The app already conditionally applies Google Services plugin when this file exists.

## 3) Add SHA Fingerprints (required for Phone Auth)

From project root on Windows:

```powershell
.\gradlew signingReport
```

Copy SHA-1 and SHA-256 for debug/release and add them in:

- Firebase Console > Project Settings > Your Android app > SHA certificate fingerprints.

## 4) Enable Phone Authentication

1. Firebase Console > Authentication > Sign-in method.
2. Enable `Phone` provider.
3. Add test phone numbers in Authentication > Settings (recommended for development).

## 5) Enable Firestore

1. Firebase Console > Firestore Database.
2. Create database (production mode recommended).
3. Choose your region.

## 6) Deploy Firestore Rules

This repository includes strict rules in `firestore.rules`.

From project root:

```powershell
npx -y firebase-tools@latest use <your-firebase-project-id>
npx -y firebase-tools@latest deploy --only firestore:rules
```

If this is your first Firebase CLI init for Firestore, run:

```powershell
npx -y firebase-tools@latest init firestore
```

When prompted, use `firestore.rules` as the rules file.

## 7) Deploy bootstrap callable function (recommended)

The Android app best-effort calls a callable function named `bootstrapUser` after sign-in.

What this step is:

- This is backend code that runs on Firebase servers, not Android app code.
- It safely creates/repairs account data after auth.
- It protects phone mapping uniqueness server-side to prevent hijacking.

If you skip this step:

- Sign-in can still work.
- But secure server-owned bootstrap for `phones/{phoneE164}` and guaranteed `wallets/{uid}` creation will be missing.

If deploy fails with this error:

- `No targets in firebase.json match '--only functions'`
- You have not initialized Cloud Functions in this project yet.

Initialize Functions once from project root:

```powershell
npx -y firebase-tools@latest init functions
```

Recommended choices during init:

- Language: JavaScript (or TypeScript if you prefer)
- ESLint: optional
- Install dependencies now: Yes

Purpose:

- Ensure `users/{uid}` exists.
- Ensure `wallets/{uid}` exists.
- Ensure `phones/{phoneE164}` mapping is unique and server-owned.

### Suggested behavior

- Use Admin SDK transaction.
- Reject if `phones/{phoneE164}` exists and points to another uid.
- Create wallet with zero balance if missing.

### Minimal Node.js callable example

```js
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.bootstrapUser = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const phoneE164 = request.data?.phoneE164 || request.auth?.token?.phone_number;
  if (!phoneE164) {
    throw new HttpsError("invalid-argument", "Missing phoneE164.");
  }

  const userRef = db.collection("users").doc(uid);
  const walletRef = db.collection("wallets").doc(uid);
  const phoneRef = db.collection("phones").doc(phoneE164);

  await db.runTransaction(async (tx) => {
    const phoneSnap = await tx.get(phoneRef);
    if (phoneSnap.exists && phoneSnap.get("uid") !== uid) {
      throw new HttpsError("already-exists", "Phone already mapped to another user.");
    }

    const now = admin.firestore.FieldValue.serverTimestamp();

    tx.set(userRef, {
      phoneE164,
      updatedAt: now,
      lastLoginAt: now,
      status: "ACTIVE",
      createdAt: now,
    }, { merge: true });

    const walletSnap = await tx.get(walletRef);
    if (!walletSnap.exists) {
      tx.set(walletRef, {
        balanceMinor: 0,
        currency: "USD",
        updatedAt: now,
      });
    }

    tx.set(phoneRef, {
      uid,
      createdAt: now,
    }, { merge: false });
  });

  return { ok: true };
});
```

Deploy functions:

```powershell
npx -y firebase-tools@latest deploy --only functions
```

## 8) Validate End-to-End Flow

1. Launch app.
2. Welcome:
   - `Create account` -> phone entry -> OTP -> profile setup -> home.
   - `Sign in` -> phone entry -> OTP -> home/profile setup based on profile completeness.
3. Home:
   - `Reset credentials` -> OTP re-verification.
   - `Sign out` -> logout confirmation -> welcome.
4. Kill and reopen app:
   - session should persist via Firebase Auth.

## 9) Troubleshooting

- `Phone verification failed`:
  - Check SHA fingerprints, Phone provider enabled, and test number config.
- App always goes to Welcome:
  - Confirm `google-services.json` exists and project matches app package.
- Callable `bootstrapUser` fails:
  - Deploy functions and verify region/default settings.
- Firestore permission errors:
  - Ensure `firestore.rules` deployed to the correct project.
