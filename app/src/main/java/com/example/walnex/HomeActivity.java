package com.example.walnex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthLocalStore;
import com.example.walnex.auth.AuthNavigator;
import com.example.walnex.transfer.TransferRecord;
import com.example.walnex.wallet.WalletDefaults;
import com.google.firebase.Timestamp;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private static final int HOME_RECENT_LIMIT = 4;   // unique recipients shown
    private static final int HOME_TX_LIMIT     = 4;   // latest transactions shown
    private static final int HOME_RECENT_FETCH = 20;  // docs fetched to find 4 unique

    // ──────────────────────────────────────────────────────────────────────────
    //  Inner model classes
    // ──────────────────────────────────────────────────────────────────────────

    public static class Recipient {
        public final String name;
        public final int avatarRes;

        public Recipient(String name, int avatarRes) {
            this.name      = name;
            this.avatarRes = avatarRes;
        }
    }

    public static class Transaction {
        public final String merchantName;
        public final String merchantType;
        public final int iconRes;
        public final boolean isCredit;
        public final double amount;
        public final String currency;
        public final long timestampMs;
        public final String txNumber;

        public Transaction(String merchantName, String merchantType, int iconRes,
                           boolean isCredit, double amount, String currency,
                           long timestampMs, String txNumber) {
            this.merchantName = merchantName;
            this.merchantType = merchantType;
            this.iconRes      = iconRes;
            this.isCredit     = isCredit;
            this.amount       = amount;
            this.currency     = currency;
            this.timestampMs  = timestampMs;
            this.txNumber     = txNumber;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────────────────

    private LinearLayout layoutRecentTransfers;
    private LinearLayout layoutTransactions;

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutRecentTransfers = findViewById(R.id.layoutRecentTransfers);
        layoutTransactions    = findViewById(R.id.layoutTransactions);

        bindUserStatus();
        bindBalance();
        setupCardActions();
        setupNavBar();
        // loadRecentTransfers() and loadLatestTransactions() are called in onResume(),
        // which always runs after onCreate() — no need to call them here too.
    }

    /**
     * Called every time this activity returns to the foreground
     * (e.g. after the user finishes a transfer and is sent back here).
        * Refreshes balance and the recent-transfers / transactions lists so
        * Firestore updates are reflected without a full restart.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh balance and lists whenever we come back to this screen
        bindBalance();
        layoutRecentTransfers.removeAllViews();
        layoutTransactions.removeAllViews();
        loadRecentTransfers();
        loadLatestTransactions();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  User / greeting
    // ──────────────────────────────────────────────────────────────────────────

    private void bindUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AuthLocalStore.clear(this);
            AuthNavigator.openWelcome(this);
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String displayName = null;
                    if (snapshot.exists()) {
                        displayName = snapshot.getString("fullName");
                    }
                    if (TextUtils.isEmpty(displayName)) {
                        String phone = user.getPhoneNumber();
                        displayName = TextUtils.isEmpty(phone) ? uid : phone;
                    }
                    setGreeting(displayName);
                })
                .addOnFailureListener(e -> {
                    String phone = user.getPhoneNumber();
                    setGreeting(TextUtils.isEmpty(phone) ? uid : phone);
                });
    }

    private void setGreeting(String name) {
        TextView textName = findViewById(R.id.textGreetingName);
        if (textName != null) {
            textName.setText(getString(R.string.home_greeting_name, name));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Balance  ← now reads from Firestore
    // ──────────────────────────────────────────────────────────────────────────

    private void bindBalance() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AuthLocalStore.clear(this);
            AuthNavigator.openWelcome(this);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("wallets")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(snapshot -> {
                Double balance = snapshot.getDouble("balance");
                String currency = snapshot.getString("currency");

                double resolvedBalance = balance != null
                    ? balance
                    : WalletDefaults.DEFAULT_BALANCE;
                String resolvedCurrency = !TextUtils.isEmpty(currency)
                    ? currency
                    : WalletDefaults.DEFAULT_CURRENCY;

                if (!snapshot.exists() || balance == null || TextUtils.isEmpty(currency)) {
                    Map<String, Object> walletPatch = new HashMap<>();
                    walletPatch.put("balance", resolvedBalance);
                    walletPatch.put("currency", resolvedCurrency);
                    walletPatch.put("updatedAt", FieldValue.serverTimestamp());
                    db.collection("wallets")
                        .document(user.getUid())
                        .set(walletPatch, SetOptions.merge());
                }

                bindBalanceViews(resolvedBalance, resolvedCurrency);
            })
            .addOnFailureListener(error ->
                bindBalanceViews(WalletDefaults.DEFAULT_BALANCE, WalletDefaults.DEFAULT_CURRENCY)
            );
    }

    private void bindBalanceViews(double balance, String currency) {
        TextView textWhole   = findViewById(R.id.textBalanceWhole);
        TextView textDecimal = findViewById(R.id.textBalanceDecimal);
        TextView textCurrency = findViewById(R.id.textBalanceCurrency);

        if (textWhole == null || textDecimal == null) return;

        long wholePart   = (long) balance;
        int decimalPart  = (int) Math.round((balance - wholePart) * 100);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        textWhole.setText(nf.format(wholePart));
        textDecimal.setText(String.format(Locale.US, ".%02d", decimalPart));

        if (textCurrency != null && !TextUtils.isEmpty(currency)) {
            textCurrency.setText(currency);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Card action buttons
    // ──────────────────────────────────────────────────────────────────────────

    private void setupCardActions() {
        findViewById(R.id.actionTopUp).setOnClickListener(v ->
                Toast.makeText(this, "Top up – coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.actionWithdraw).setOnClickListener(v ->
                Toast.makeText(this, "Withdraw – coming soon", Toast.LENGTH_SHORT).show());

        // Transfer: navigate to TransferToActivity
        findViewById(R.id.actionTransfer).setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferToActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Settings icon
        findViewById(R.id.imageSettings).setOnClickListener(v -> {
            startActivity(SettingsActivity.newIntent(this));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bottom navigation
    // ──────────────────────────────────────────────────────────────────────────

    private void setupNavBar() {
        View navHistory = findViewById(R.id.navHistory);
        View navCards   = findViewById(R.id.navCards);
        View navMore    = findViewById(R.id.navMore);

        if (navHistory != null) navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        if (navCards != null) navCards.setOnClickListener(v -> {
            startActivity(new Intent(this, CardsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        if (navMore != null) navMore.setOnClickListener(v -> {
            startActivity(new Intent(this, MoreActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        TextView viewAll = findViewById(R.id.textViewAll);
        if (viewAll != null) {
            viewAll.setOnClickListener(v -> {
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void showSignOutOptions() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.auth_sign_out)
                .setMessage(R.string.auth_sign_out_confirmation)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.auth_sign_out, (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    AuthLocalStore.clear(this);
                    AuthNavigator.openWelcome(this);
                })
                .show();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Recent transfers  ← now reads from Firestore
    // ──────────────────────────────────────────────────────────────────────────

    private void loadRecentTransfers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AuthLocalStore.clear(this);
            AuthNavigator.openWelcome(this);
            return;
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .collection("transactions")
            .orderBy(TransferRecord.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(HOME_RECENT_FETCH)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Recipient> recipients = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    String counterpartyName = resolveCounterpartyName(doc, user.getUid());
                    if (TextUtils.isEmpty(counterpartyName)) continue;

                    // Use UID as dedup key for WalNex users; fall back to name for
                    // external contacts whose UID is empty.
                    String counterpartyUid = resolveCounterpartyUid(doc, user.getUid());
                    String dedupeKey = !TextUtils.isEmpty(counterpartyUid)
                            ? counterpartyUid
                            : counterpartyName;
                    if (seen.contains(dedupeKey)) continue;

                    seen.add(dedupeKey);
                    recipients.add(new Recipient(counterpartyName, 0));
                    if (recipients.size() >= HOME_RECENT_LIMIT) break;
                }
                renderRecentTransfers(recipients);
            })
            .addOnFailureListener(error -> renderRecentTransfers(new ArrayList<>()));
    }

    private void renderRecentTransfers(List<Recipient> recipients) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Recipient recipient : recipients) {
            View itemView = inflater.inflate(
                    R.layout.item_recipient, layoutRecentTransfers, false);

            ImageView avatar = itemView.findViewById(R.id.imageRecipientAvatar);
            TextView  name   = itemView.findViewById(R.id.textRecipientName);

            name.setText(recipient.name);
            if (recipient.avatarRes != 0) {
                avatar.setImageResource(recipient.avatarRes);
            } else {
                avatar.setImageResource(R.drawable.ic_person_avatar);
            }

            itemView.setOnClickListener(v ->
                    Toast.makeText(this,
                            "Transfer to " + recipient.name, Toast.LENGTH_SHORT).show());

            layoutRecentTransfers.addView(itemView);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Latest transactions  ← Firestore query (newest first)
    // ──────────────────────────────────────────────────────────────────────────

    private void loadLatestTransactions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AuthLocalStore.clear(this);
            AuthNavigator.openWelcome(this);
            return;
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .collection("transactions")
            .orderBy(TransferRecord.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(HOME_TX_LIMIT)   // fetch exactly 4
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Transaction> txList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    Transaction tx = mapTransferTransaction(doc, user.getUid());
                    if (tx != null) txList.add(tx);
                }
                renderTransactions(txList);
            })
            .addOnFailureListener(error -> renderTransactions(new ArrayList<>()));
    }

    private void renderTransactions(List<Transaction> txList) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < txList.size(); i++) {
            Transaction tx = txList.get(i);
            View rowView = inflater.inflate(
                    R.layout.item_transaction_row, layoutTransactions, false);
            bindTransactionRow(rowView, tx);
            rowView.setOnClickListener(v -> showTransactionDetail(tx));
            layoutTransactions.addView(rowView);

            if (i < txList.size() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMarginStart(62);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(getColor(R.color.home_divider));
                layoutTransactions.addView(divider);
            }
        }
    }

    private Transaction mapTransferTransaction(QueryDocumentSnapshot doc, String myUid) {
        Double amount = doc.getDouble(TransferRecord.FIELD_AMOUNT);
        if (amount == null) {
            return null;
        }

        String senderUid = doc.getString(TransferRecord.FIELD_SENDER_UID);
        boolean isCredit = senderUid == null || !senderUid.equals(myUid);

        String counterparty = resolveCounterpartyName(doc, myUid);
        if (TextUtils.isEmpty(counterparty)) {
            counterparty = getString(R.string.home_action_transfer);
        }

        String currency = doc.getString(TransferRecord.FIELD_CURRENCY);
        if (TextUtils.isEmpty(currency)) {
            currency = WalletDefaults.DEFAULT_CURRENCY;
        }

        Timestamp timestamp = doc.getTimestamp(TransferRecord.FIELD_CREATED_AT);
        long timestampMs = timestamp != null
            ? timestamp.toDate().getTime()
            : System.currentTimeMillis();

        String txId = doc.getString(TransferRecord.FIELD_TX_ID);
        if (TextUtils.isEmpty(txId)) {
            txId = doc.getId();
        }

        return new Transaction(
            counterparty,
            getString(R.string.home_action_transfer),
            R.drawable.ic_tx_generic,
            isCredit,
            amount,
            currency,
            timestampMs,
            txId
        );
    }

    private String resolveCounterpartyUid(QueryDocumentSnapshot doc, String myUid) {
        String senderUid    = doc.getString(TransferRecord.FIELD_SENDER_UID);
        String recipientUid = doc.getString(TransferRecord.FIELD_RECIPIENT_UID);
        if (TextUtils.isEmpty(senderUid)) return null;
        // recipientUid may be empty for external (non-WalNex) contacts — that is OK.
        return myUid.equals(senderUid) ? recipientUid : senderUid;
    }

    private String resolveCounterpartyName(QueryDocumentSnapshot doc, String myUid) {
        String senderUid = doc.getString(TransferRecord.FIELD_SENDER_UID);
        boolean isSender = senderUid != null && senderUid.equals(myUid);
        String name = isSender
            ? doc.getString(TransferRecord.FIELD_RECIPIENT_NAME)
            : doc.getString(TransferRecord.FIELD_SENDER_NAME);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        return isSender
            ? doc.getString(TransferRecord.FIELD_RECIPIENT_PHONE)
            : doc.getString(TransferRecord.FIELD_SENDER_PHONE);
    }

    private void bindTransactionRow(View rowView, Transaction tx) {
        ImageView icon   = rowView.findViewById(R.id.imageTxIcon);
        TextView  name   = rowView.findViewById(R.id.textTxName);
        TextView  date   = rowView.findViewById(R.id.textTxDate);
        TextView  amount = rowView.findViewById(R.id.textTxAmount);

        name.setText(tx.merchantName);
        date.setText(formatRelativeDate(tx.timestampMs));
        icon.setImageResource(tx.iconRes);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String formattedAmount = nf.format(tx.amount);

        if (tx.isCredit) {
            amount.setText("+" + tx.currency + " " + formattedAmount);
            amount.setTextColor(getColor(R.color.tx_credit_text));
        } else {
            amount.setText("-" + tx.currency + " " + formattedAmount);
            amount.setTextColor(getColor(R.color.tx_debit_text));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Transaction detail bottom sheet
    // ──────────────────────────────────────────────────────────────────────────

    private void showTransactionDetail(Transaction tx) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_transaction_detail, null);
        dialog.setContentView(sheetView);

        ImageView merchantIcon = sheetView.findViewById(R.id.imageDetailMerchantIcon);
        TextView  merchantName = sheetView.findViewById(R.id.textDetailMerchantName);
        TextView  merchantType = sheetView.findViewById(R.id.textDetailMerchantType);
        merchantIcon.setImageResource(tx.iconRes);
        merchantName.setText(tx.merchantName);
        merchantType.setText(tx.merchantType);

        FrameLayout amountFrame = sheetView.findViewById(R.id.frameDetailAmount);
        TextView    amountText  = sheetView.findViewById(R.id.textDetailAmount);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String formatted = nf.format(tx.amount);
        if (tx.isCredit) {
            amountText.setText("+" + tx.currency + " " + formatted);
            amountText.setTextColor(getColor(R.color.tx_credit_text));
            amountFrame.setBackgroundResource(R.drawable.bg_tx_amount_credit);
        } else {
            amountText.setText("-" + tx.currency + " " + formatted);
            amountText.setTextColor(getColor(R.color.tx_debit_text));
            amountFrame.setBackgroundResource(R.drawable.bg_tx_amount_debit);
        }

        TextView dateRelative = sheetView.findViewById(R.id.textDetailDateRelative);
        TextView dateAbsolute = sheetView.findViewById(R.id.textDetailDateAbsolute);
        dateRelative.setText(formatRelativeWords(tx.timestampMs));
        dateAbsolute.setText(formatAbsoluteDate(tx.timestampMs));

        TextView txNumber = sheetView.findViewById(R.id.textDetailTxNumber);
        txNumber.setText(tx.txNumber);

        sheetView.findViewById(R.id.imageCopyTxNumber).setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("tx_number", tx.txNumber);
            cm.setPrimaryClip(clip);
            Toast.makeText(this, R.string.tx_copied, Toast.LENGTH_SHORT).show();
        });

        sheetView.findViewById(R.id.textDetailDone).setOnClickListener(v -> dialog.dismiss());

        sheetView.findViewById(R.id.layoutDetailReport).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Report submitted – coming soon", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Date / time helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String formatRelativeDate(long timestampMs) {
        long nowMs   = System.currentTimeMillis();
        long diffMs  = nowMs - timestampMs;
        long diffDays = diffMs / (24L * 60 * 60 * 1000);

        java.util.Date date = new java.util.Date(timestampMs);
        java.text.SimpleDateFormat timeFmt =
                new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFmt.format(date);

        if (diffDays == 0 && isSameDay(timestampMs, nowMs)) {
            return "Today " + timeStr;
        } else if (diffDays == 1 || (!isSameDay(timestampMs, nowMs) && diffDays < 2)) {
            return "Yesterday " + timeStr;
        } else {
            java.text.SimpleDateFormat dateFmt =
                    new java.text.SimpleDateFormat("MMM dd", Locale.getDefault());
            return dateFmt.format(date) + " " + timeStr;
        }
    }

    private String formatRelativeWords(long timestampMs) {
        long nowMs    = System.currentTimeMillis();
        long diffMs   = nowMs - timestampMs;
        long diffDays = diffMs / (24L * 60 * 60 * 1000);

        if (isSameDay(timestampMs, nowMs)) return "Today";
        if (diffDays == 1 || (!isSameDay(timestampMs, nowMs) && diffDays < 2))
            return "Yesterday";
        if (diffDays < 7)  return diffDays + " days ago";
        if (diffDays < 14) return "1 week ago";
        if (diffDays < 30) return (diffDays / 7) + " weeks ago";
        return (diffDays / 30) + " months ago";
    }

    private String formatAbsoluteDate(long timestampMs) {
        java.util.Date date = new java.util.Date(timestampMs);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                "MMM d, yyyy - HH:mm", Locale.getDefault());
        return fmt.format(date);
    }

    private boolean isSameDay(long ms1, long ms2) {
        java.util.Calendar c1 = java.util.Calendar.getInstance();
        java.util.Calendar c2 = java.util.Calendar.getInstance();
        c1.setTimeInMillis(ms1);
        c2.setTimeInMillis(ms2);
        return c1.get(java.util.Calendar.YEAR)        == c2.get(java.util.Calendar.YEAR)
                && c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR);
    }
}