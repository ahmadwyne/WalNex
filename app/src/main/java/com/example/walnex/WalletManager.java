package com.example.walnex;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Local wallet state manager backed by SharedPreferences.
 *
 * Replaces Firestore balance reads/writes during local development.
 * Stores:
 *   • Current balance  (double)
 *   • Recent transfers (JSON array, max 10 entries, most-recent first)
 *   • Recent transactions (JSON array, max 20 entries, most-recent first)
 */
public final class WalletManager {

    private static final String PREFS_NAME         = "walnex_wallet";
    private static final String KEY_BALANCE        = "balance";
    private static final String KEY_RECENT_XFERS   = "recent_transfers";
    private static final String KEY_RECENT_TXS     = "recent_transactions";

    /** Default balance assigned on first launch (mirrors the hardcoded value in HomeActivity). */
    public static final double DEFAULT_BALANCE = 14_235.34;

    // ── Public model ──────────────────────────────────────────────────────────

    /** Lightweight transfer recipient entry stored in recent-transfers list. */
    public static class RecentTransfer {
        public final String name;
        public final String phone;
        public final int    avatarRes;   // 0 = use default avatar

        public RecentTransfer(String name, String phone, int avatarRes) {
            this.name      = name;
            this.phone     = phone;
            this.avatarRes = avatarRes;
        }
    }

    /** Lightweight transaction entry stored in recent-transactions list. */
    public static class LocalTransaction {
        public final String merchantName;
        public final String merchantType;
        public final boolean isCredit;
        public final double  amount;
        public final String  currency;
        public final long    timestampMs;
        public final String  txId;

        public LocalTransaction(String merchantName, String merchantType,
                                boolean isCredit, double amount, String currency,
                                long timestampMs, String txId) {
            this.merchantName = merchantName;
            this.merchantType = merchantType;
            this.isCredit     = isCredit;
            this.amount       = amount;
            this.currency     = currency;
            this.timestampMs  = timestampMs;
            this.txId         = txId;
        }
    }

    // ── Singleton prefs accessor ──────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    /** Returns the stored balance, seeding it with {@link #DEFAULT_BALANCE} on first call. */
    public static double getBalance(Context ctx) {
        SharedPreferences sp = prefs(ctx);
        if (!sp.contains(KEY_BALANCE)) {
            // First launch — seed with default
            sp.edit().putString(KEY_BALANCE, String.valueOf(DEFAULT_BALANCE)).apply();
            return DEFAULT_BALANCE;
        }
        try {
            return Double.parseDouble(sp.getString(KEY_BALANCE, String.valueOf(DEFAULT_BALANCE)));
        } catch (NumberFormatException e) {
            return DEFAULT_BALANCE;
        }
    }

    /** Overwrites the stored balance. */
    public static void setBalance(Context ctx, double balance) {
        prefs(ctx).edit().putString(KEY_BALANCE, String.valueOf(balance)).apply();
    }

    /**
     * Debits {@code amount} from the stored balance.
     *
     * @throws InsufficientFundsException if current balance < amount.
     */
    public static void debitBalance(Context ctx, double amount) throws InsufficientFundsException {
        double current = getBalance(ctx);
        if (current < amount) {
            throw new InsufficientFundsException(
                    "Balance " + current + " is less than " + amount);
        }
        setBalance(ctx, current - amount);
    }

    // ── Recent transfers ──────────────────────────────────────────────────────

    private static final int MAX_RECENT_TRANSFERS = 10;

    /** Returns the most-recent-first list of transfer recipients (up to 10). */
    public static List<RecentTransfer> getRecentTransfers(Context ctx) {
        List<RecentTransfer> list = new ArrayList<>();
        String json = prefs(ctx).getString(KEY_RECENT_XFERS, null);
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new RecentTransfer(
                        o.getString("name"),
                        o.optString("phone", ""),
                        o.optInt("avatarRes", 0)));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    /**
     * Prepends a new recipient to the recent-transfers list.
     * Deduplicates by name+phone, keeping the freshest entry at the front.
     */
    public static void pushRecentTransfer(Context ctx, RecentTransfer recipient) {
        List<RecentTransfer> existing = getRecentTransfers(ctx);

        // Remove any existing entry for the same contact so we don't show duplicates
        existing.removeIf(r ->
                r.name.equals(recipient.name) && r.phone.equals(recipient.phone));

        // Prepend new entry
        existing.add(0, recipient);

        // Trim to max size
        if (existing.size() > MAX_RECENT_TRANSFERS) {
            existing = existing.subList(0, MAX_RECENT_TRANSFERS);
        }

        // Serialize back
        JSONArray arr = new JSONArray();
        try {
            for (RecentTransfer r : existing) {
                JSONObject o = new JSONObject();
                o.put("name",      r.name);
                o.put("phone",     r.phone);
                o.put("avatarRes", r.avatarRes);
                arr.put(o);
            }
        } catch (JSONException ignored) {}

        prefs(ctx).edit().putString(KEY_RECENT_XFERS, arr.toString()).apply();
    }

    // ── Recent transactions ───────────────────────────────────────────────────

    private static final int MAX_RECENT_TXS = 20;

    /** Returns the most-recent-first list of local transactions (up to 20). */
    public static List<LocalTransaction> getRecentTransactions(Context ctx) {
        List<LocalTransaction> list = new ArrayList<>();
        String json = prefs(ctx).getString(KEY_RECENT_TXS, null);
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new LocalTransaction(
                        o.getString("merchantName"),
                        o.getString("merchantType"),
                        o.getBoolean("isCredit"),
                        o.getDouble("amount"),
                        o.getString("currency"),
                        o.getLong("timestampMs"),
                        o.getString("txId")));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    /** Prepends a new transaction to the local history list. */
    public static void pushTransaction(Context ctx, LocalTransaction tx) {
        List<LocalTransaction> existing = getRecentTransactions(ctx);
        existing.add(0, tx);

        if (existing.size() > MAX_RECENT_TXS) {
            existing = existing.subList(0, MAX_RECENT_TXS);
        }

        JSONArray arr = new JSONArray();
        try {
            for (LocalTransaction t : existing) {
                JSONObject o = new JSONObject();
                o.put("merchantName", t.merchantName);
                o.put("merchantType", t.merchantType);
                o.put("isCredit",     t.isCredit);
                o.put("amount",       t.amount);
                o.put("currency",     t.currency);
                o.put("timestampMs",  t.timestampMs);
                o.put("txId",         t.txId);
                arr.put(o);
            }
        } catch (JSONException ignored) {}

        prefs(ctx).edit().putString(KEY_RECENT_TXS, arr.toString()).apply();
    }

    // ── Custom exception ──────────────────────────────────────────────────────

    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) { super(message); }
    }

    // ── Private constructor ───────────────────────────────────────────────────
    private WalletManager() {}
}