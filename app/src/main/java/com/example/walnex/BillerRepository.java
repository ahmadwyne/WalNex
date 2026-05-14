package com.example.walnex;

import com.example.walnex.transfer.TransferRecord;
import com.example.walnex.wallet.WalletDefaults;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public final class BillerRepository {

    private static final String COL_USERS   = "users";
    private static final String COL_BILLERS = "billers";
    private static final String COL_WALLETS = "wallets";
    private static final String COL_TX      = "transactions";

    private BillerRepository() {}

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public static Task<DocumentReference> addBiller(String category, String accountNumber,
                                                     double dueAmount, long dueDateMs) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        Map<String, Object> data = new HashMap<>();
        data.put("category",      category);
        data.put("accountNumber", accountNumber);
        data.put("dueAmount",     dueAmount);
        data.put("dueDateMs",     dueDateMs);
        data.put("createdAt",     FieldValue.serverTimestamp());

        return FirebaseFirestore.getInstance()
                .collection(COL_USERS).document(user.getUid())
                .collection(COL_BILLERS)
                .add(data);
    }

    public static Task<QuerySnapshot> loadBillers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        return FirebaseFirestore.getInstance()
                .collection(COL_USERS).document(user.getUid())
                .collection(COL_BILLERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public static Task<Void> deleteBiller(String billerId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        return FirebaseFirestore.getInstance()
                .collection(COL_USERS).document(user.getUid())
                .collection(COL_BILLERS).document(billerId)
                .delete();
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    /**
     * Atomically debits the sender wallet and records a bill_payment transaction.
     *
     * @return Task resolving to the generated transaction ID on success.
     */
    public static Task<String> submitBillPayment(BillerModel biller,
                                                  double amount,
                                                  String currency) {
        FirebaseUser sender = FirebaseAuth.getInstance().getCurrentUser();
        if (sender == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference walletRef = db.collection(COL_WALLETS).document(sender.getUid());
        DocumentReference profileRef = db.collection(COL_USERS).document(sender.getUid());
        DocumentReference txRef = db.collection(COL_USERS).document(sender.getUid())
                .collection(COL_TX).document();
        final String txId = txRef.getId();

        return db.runTransaction(transaction -> {

            DocumentSnapshot wallet  = transaction.get(walletRef);
            DocumentSnapshot profile = transaction.get(profileRef);

            double balance = WalletDefaults.DEFAULT_BALANCE;
            if (wallet.exists()) {
                Double b = wallet.getDouble("balance");
                if (b != null) balance = b;
            }

            if (balance < amount) {
                throw new InsufficientFundsException("Insufficient balance");
            }

            String senderName  = profile.getString("fullName");
            String senderPhone = profile.getString("phoneE164");
            if (senderName  == null) senderName  = sender.getUid();
            if (senderPhone == null) senderPhone = "";

            Map<String, Object> payload = new HashMap<>();
            payload.put(TransferRecord.FIELD_TX_ID,           txId);
            payload.put(TransferRecord.FIELD_SENDER_UID,      sender.getUid());
            payload.put(TransferRecord.FIELD_SENDER_NAME,     senderName);
            payload.put(TransferRecord.FIELD_SENDER_PHONE,    senderPhone);
            payload.put(TransferRecord.FIELD_RECIPIENT_UID,   "");
            payload.put(TransferRecord.FIELD_RECIPIENT_PHONE, biller.accountNumber);
            payload.put(TransferRecord.FIELD_RECIPIENT_NAME,  biller.category);
            payload.put(TransferRecord.FIELD_AMOUNT,          amount);
            payload.put(TransferRecord.FIELD_CURRENCY,        currency);
            payload.put(TransferRecord.FIELD_STATUS,          TransferRecord.STATUS_SUCCESS);
            payload.put(TransferRecord.FIELD_TYPE,            "bill_payment");
            payload.put(TransferRecord.FIELD_CREATED_AT,      FieldValue.serverTimestamp());

            Map<String, Object> walletUpdate = new HashMap<>();
            walletUpdate.put("balance",   balance - amount);
            walletUpdate.put("currency",  currency);
            walletUpdate.put("updatedAt", FieldValue.serverTimestamp());

            if (wallet.exists()) {
                transaction.update(walletRef, walletUpdate);
            } else {
                transaction.set(walletRef, walletUpdate);
            }

            transaction.set(txRef, payload);
            return txId;
        });
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) { super(message); }
    }
}
