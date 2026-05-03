package com.example.walnex.transfer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for all transfer-feature backend operations.
 *
 * Running on the Firebase Spark (free) tier — no Cloud Functions available.
 * Transfer logic is executed as a client-side Firestore transaction:
 *   1. Read sender wallet balance.
 *   2. Validate sufficient funds.
 *   3. Atomically debit sender, credit recipient, and write both ledger entries.
 */
public final class TransferRepository {

    private static final String TAG = "TransferRepository";

    private static final String COL_USERS        = "users";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_WALLETS      = "wallets";

    public static final int MAX_FREQUENT = 3;

    private TransferRepository() {}

    // ── Submit transfer (client-side atomic Firestore transaction) ────────────

    /**
     * Transfers money atomically without a Cloud Function:
     *
     *  1. Reads the sender's wallet balance inside a transaction.
     *  2. Throws {@link InsufficientFundsException} and rolls back if underfunded.
     *  3. On success, atomically:
     *       • Debits the sender wallet.
     *       • Credits the recipient wallet.
     *       • Writes a SUCCESS transaction record on the sender's ledger.
     *       • Writes a SUCCESS transaction record on the recipient's ledger.
     *
     * @return Task resolving to the shared transaction document ID on success.
     */
    public static Task<String> submitTransfer(
            TransferContact recipient,
            double amount,
            String currency) {

        FirebaseUser sender = FirebaseAuth.getInstance().getCurrentUser();
        if (sender == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Wallet refs
        DocumentReference senderWalletRef    = db.collection(COL_WALLETS).document(sender.getUid());
        DocumentReference recipientWalletRef = db.collection(COL_WALLETS).document(recipient.uid);

        // Pre-generate a shared transaction ID so both ledger entries are linked
        DocumentReference senderTxRef = db
                .collection(COL_USERS).document(sender.getUid())
                .collection(COL_TRANSACTIONS).document();         // auto-ID
        final String txId = senderTxRef.getId();

        DocumentReference recipientTxRef = db
                .collection(COL_USERS).document(recipient.uid)
                .collection(COL_TRANSACTIONS).document(txId);    // same ID

        return db.runTransaction(transaction -> {

            // 1. Read sender wallet
            DocumentSnapshot senderWallet = transaction.get(senderWalletRef);
            double currentBalance = 0;
            if (senderWallet.exists()) {
                Double b = senderWallet.getDouble("balance");
                if (b != null) currentBalance = b;
            }

            // 2. Validate funds
            if (currentBalance < amount) {
                throw new InsufficientFundsException(
                        "Balance " + currentBalance + " " + currency
                                + " is less than " + amount + " " + currency);
            }

            // 3. Read recipient wallet (needed to compute new balance)
            DocumentSnapshot recipientWallet = transaction.get(recipientWalletRef);
            double recipientBalance = 0;
            if (recipientWallet.exists()) {
                Double b = recipientWallet.getDouble("balance");
                if (b != null) recipientBalance = b;
            }

            // 4. Build shared ledger payload (status = success, no pending step needed)
            Map<String, Object> txPayload = TransferRecord.newSuccessPayload(
                    sender.getUid(),
                    recipient.uid,
                    recipient.phoneE164,
                    recipient.fullName,
                    amount,
                    currency,
                    txId);

            // 5. Apply all writes atomically
            transaction.update(senderWalletRef,    "balance", currentBalance - amount);
            transaction.update(recipientWalletRef, "balance", recipientBalance + amount);
            transaction.set(senderTxRef,    txPayload);
            transaction.set(recipientTxRef, txPayload);

            return txId;

        }).addOnFailureListener(e -> Log.w(TAG, "submitTransfer failed", e));
    }

    // ── Device contacts ───────────────────────────────────────────────────────

    /**
     * Reads all contacts from the device phonebook synchronously.
     * <b>Call from a background thread.</b> Requires READ_CONTACTS permission.
     */
    public static List<TransferContact> loadDeviceContacts(Context context) {
        List<TransferContact> contacts = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC")) {

            if (cursor == null) return contacts;

            int colKey  = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY);
            int colName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
            int colNorm = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
            int colRaw  = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                String lookupKey = cursor.getString(colKey);
                if (TextUtils.isEmpty(lookupKey) || seenKeys.contains(lookupKey)) continue;
                seenKeys.add(lookupKey);

                String name  = cursor.getString(colName);
                String phone = cursor.getString(colNorm);
                if (TextUtils.isEmpty(phone)) phone = cursor.getString(colRaw);
                if (TextUtils.isEmpty(name)) continue;

                contacts.add(new TransferContact(lookupKey, name, phone != null ? phone : "", 0));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadDeviceContacts failed", e);
        }

        return contacts;
    }

    /**
     * Reads a single contact from a URI returned by the system contact picker.
     */
    public static TransferContact readContactFromUri(Context context, Uri contactUri) {
        String lookupKey = null, name = null, phone = null;

        try (Cursor c = context.getContentResolver().query(contactUri,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                }, null, null, null)) {

            if (c == null || !c.moveToFirst()) return null;

            long id = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            lookupKey = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
            name      = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));

            try (Cursor pc = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{String.valueOf(id)}, null)) {

                if (pc != null && pc.moveToFirst()) {
                    phone = pc.getString(pc.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                    if (TextUtils.isEmpty(phone)) {
                        phone = pc.getString(pc.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readContactFromUri failed", e);
            return null;
        }

        if (TextUtils.isEmpty(name)) return null;
        return new TransferContact(
                lookupKey != null ? lookupKey : "",
                name,
                phone != null ? phone : "",
                0);
    }

    // ── Frequent contacts (Firestore) ─────────────────────────────────────────

    public static Task<List<TransferContact>> loadFrequentContacts() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return Tasks.forResult(Collections.emptyList());

        return FirebaseFirestore.getInstance()
                .collection(COL_USERS).document(me.getUid())
                .collection(COL_TRANSACTIONS)
                .whereEqualTo(TransferRecord.FIELD_TYPE,   TransferRecord.TYPE_TRANSFER)
                .whereEqualTo(TransferRecord.FIELD_STATUS, TransferRecord.STATUS_SUCCESS)
                .orderBy(TransferRecord.FIELD_CREATED_AT,  Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return Tasks.forResult(Collections.emptyList());
                    }
                    List<String> orderedUids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String uid = doc.getString(TransferRecord.FIELD_RECIPIENT_UID);
                        if (!TextUtils.isEmpty(uid) && !orderedUids.contains(uid)) {
                            orderedUids.add(uid);
                            if (orderedUids.size() == MAX_FREQUENT) break;
                        }
                    }
                    if (orderedUids.isEmpty()) return Tasks.forResult(Collections.emptyList());
                    List<Task<TransferContact>> fetches = new ArrayList<>();
                    for (String uid : orderedUids) fetches.add(fetchContactByUid(uid));
                    return Tasks.whenAllSuccess(fetches);
                });
    }

    public static Task<TransferContact> findContactByPhone(String phoneE164) {
        return FirebaseFirestore.getInstance()
                .collection(COL_USERS)
                .whereEqualTo("phoneE164", phoneE164)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null
                            || task.getResult().isEmpty()) return null;
                    return docToContact(
                            (QueryDocumentSnapshot) task.getResult().getDocuments().get(0));
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Task<TransferContact> fetchContactByUid(String uid) {
        return FirebaseFirestore.getInstance()
                .collection(COL_USERS).document(uid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null
                            || !task.getResult().exists()) return null;
                    String fullName = task.getResult().getString("fullName");
                    String phone    = task.getResult().getString("phoneE164");
                    if (TextUtils.isEmpty(fullName)) return null;
                    return new TransferContact(uid, fullName, phone != null ? phone : "", 0);
                });
    }

    private static TransferContact docToContact(QueryDocumentSnapshot doc) {
        if (doc == null) return null;
        String fullName = doc.getString("fullName");
        if (TextUtils.isEmpty(fullName)) return null;
        String phone = doc.getString("phoneE164");
        return new TransferContact(doc.getId(), fullName, phone != null ? phone : "", 0);
    }

    // ── Custom exceptions ─────────────────────────────────────────────────────

    // UPDATED: Now extends RuntimeException instead of Exception
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) { super(message); }
    }
}