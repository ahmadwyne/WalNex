package com.example.walnex.transfer;

import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a completed transfer transaction stored in Firestore.
 *
 * Firestore paths (both written atomically by the client transaction):
 *   users/{senderUid}/transactions/{txId}    — sender's ledger
 *   users/{recipientUid}/transactions/{txId} — recipient's ledger
 *
 * Note: on the Spark (free) tier there is no Cloud Function to validate
 * server-side, so the client writes STATUS_SUCCESS directly after the
 * balance check passes inside a Firestore transaction.
 */
public class TransferRecord {

    // Firestore field name constants
    public static final String FIELD_SENDER_UID      = "senderUid";
    public static final String FIELD_RECIPIENT_UID   = "recipientUid";
    public static final String FIELD_RECIPIENT_PHONE = "recipientPhone";
    public static final String FIELD_RECIPIENT_NAME  = "recipientName";
    public static final String FIELD_AMOUNT          = "amount";
    public static final String FIELD_CURRENCY        = "currency";
    public static final String FIELD_STATUS          = "status";
    public static final String FIELD_TYPE            = "type";
    public static final String FIELD_CREATED_AT      = "createdAt";
    public static final String FIELD_TX_ID           = "txId";

    public static final String TYPE_TRANSFER  = "transfer";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED  = "failed";

    // ── Instance fields (used when reading back from Firestore) ──────────────

    public String senderUid;
    public String recipientUid;
    public String recipientPhone;
    public String recipientName;
    public double amount;
    public String currency;
    public String status;
    public String type;
    public String txId;
    public Date   createdAt;

    /** No-arg constructor required by Firestore auto-deserialization. */
    public TransferRecord() {}

    // ── Factories ─────────────────────────────────────────────────────────────

    /**
     * Builds a SUCCESS payload for a client-side atomic transaction.
     *
     * Because the balance check and write happen atomically inside
     * {@code db.runTransaction()}, we write directly as SUCCESS — there is
     * no intermediate PENDING state when running without Cloud Functions.
     *
     * Both the sender's and recipient's ledger entries use this same map.
     *
     * @param txId The pre-generated document ID shared by both ledger entries.
     */
    public static Map<String, Object> newSuccessPayload(
            String senderUid,
            String recipientUid,
            String recipientPhone,
            String recipientName,
            double amount,
            String currency,
            String txId) {

        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_TX_ID,           txId);
        map.put(FIELD_SENDER_UID,      senderUid);
        map.put(FIELD_RECIPIENT_UID,   recipientUid);
        map.put(FIELD_RECIPIENT_PHONE, recipientPhone);
        map.put(FIELD_RECIPIENT_NAME,  recipientName);
        map.put(FIELD_AMOUNT,          amount);
        map.put(FIELD_CURRENCY,        currency);
        map.put(FIELD_STATUS,          STATUS_SUCCESS);
        map.put(FIELD_TYPE,            TYPE_TRANSFER);
        map.put(FIELD_CREATED_AT,      FieldValue.serverTimestamp());
        return map;
    }

    /**
     * Kept for reference / future Cloud Function use.
     * Not used in the Spark-tier client-side flow.
     */
    public static Map<String, Object> newPendingPayload(
            String senderUid,
            String recipientUid,
            String recipientPhone,
            String recipientName,
            double amount,
            String currency) {

        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_SENDER_UID,      senderUid);
        map.put(FIELD_RECIPIENT_UID,   recipientUid);
        map.put(FIELD_RECIPIENT_PHONE, recipientPhone);
        map.put(FIELD_RECIPIENT_NAME,  recipientName);
        map.put(FIELD_AMOUNT,          amount);
        map.put(FIELD_CURRENCY,        currency);
        map.put(FIELD_STATUS,          STATUS_PENDING);
        map.put(FIELD_TYPE,            TYPE_TRANSFER);
        map.put(FIELD_CREATED_AT,      FieldValue.serverTimestamp());
        return map;
    }
}