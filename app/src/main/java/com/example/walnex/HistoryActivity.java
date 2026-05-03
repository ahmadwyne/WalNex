package com.example.walnex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    // ── RecyclerView item types ────────────────────────────────────────────────
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TRANSACTION = 1;
    private static final int VIEW_TYPE_DIVIDER = 2;

    // ── Model: flat list item ─────────────────────────────────────────────────
    static abstract class ListItem {
        abstract int type();
    }

    static class HeaderItem extends ListItem {
        final String label;
        final boolean showTopDivider;

        HeaderItem(String label, boolean showTopDivider) {
            this.label = label;
            this.showTopDivider = showTopDivider;
        }

        @Override int type() { return VIEW_TYPE_HEADER; }
    }

    static class TransactionItem extends ListItem {
        final HomeActivity.Transaction tx;

        TransactionItem(HomeActivity.Transaction tx) { this.tx = tx; }

        @Override int type() { return VIEW_TYPE_TRANSACTION; }
    }

    static class DividerItem extends ListItem {
        @Override int type() { return VIEW_TYPE_DIVIDER; }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HomeActivity.Transaction> allTransactions;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.historyRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(null); // avoid flicker on search filter

        allTransactions = buildDummyTransactions();
        adapter = new HistoryAdapter(buildFlatList(allTransactions));
        recyclerView.setAdapter(adapter);

        setupSearch();
        setupNavBar();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        EditText searchField = findViewById(R.id.editHistorySearch);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString().trim());
            }
        });

        // Filter button — show a simple toast for now; hook up a full filter sheet later
        findViewById(R.id.buttonFilter).setOnClickListener(v ->
                Toast.makeText(this, "Filter options – coming soon", Toast.LENGTH_SHORT).show());
    }

    private void filterTransactions(String query) {
        if (query.isEmpty()) {
            adapter.updateList(buildFlatList(allTransactions));
            return;
        }
        String lower = query.toLowerCase(Locale.getDefault());
        List<HomeActivity.Transaction> filtered = new ArrayList<>();
        for (HomeActivity.Transaction tx : allTransactions) {
            if (tx.merchantName.toLowerCase(Locale.getDefault()).contains(lower)
                    || tx.merchantType.toLowerCase(Locale.getDefault()).contains(lower)) {
                filtered.add(tx);
            }
        }
        adapter.updateList(buildFlatList(filtered));
    }

    // ── Nav bar ───────────────────────────────────────────────────────────────

    private void setupNavBar() {

        View navHome = findViewById(R.id.navHome);
        View navCards = findViewById(R.id.navCards);
        View navMore = findViewById(R.id.navMore);

        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        if (navCards != null) navCards.setOnClickListener(v -> {
            startActivity(new Intent(this, CardsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        if (navMore != null) navMore.setOnClickListener(v -> {
            startActivity(new Intent(this, MoreActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    // ── Grouping logic ────────────────────────────────────────────────────────

    /**
     * Groups transactions by day label and builds a flat list containing
     * section headers, transaction rows, and thin dividers between rows.
     */
    private List<ListItem> buildFlatList(List<HomeActivity.Transaction> txList) {
        // Group by section label (preserving insertion order)
        LinkedHashMap<String, List<HomeActivity.Transaction>> grouped = new LinkedHashMap<>();
        for (HomeActivity.Transaction tx : txList) {
            String label = sectionLabel(tx.timestampMs);
            if (!grouped.containsKey(label)) {
                grouped.put(label, new ArrayList<>());
            }
            grouped.get(label).add(tx);
        }

        List<ListItem> flat = new ArrayList<>();
        boolean firstSection = true;
        for (Map.Entry<String, List<HomeActivity.Transaction>> entry : grouped.entrySet()) {
            flat.add(new HeaderItem(entry.getKey(), !firstSection));
            firstSection = false;
            List<HomeActivity.Transaction> rows = entry.getValue();
            for (int i = 0; i < rows.size(); i++) {
                flat.add(new TransactionItem(rows.get(i)));
                if (i < rows.size() - 1) {
                    flat.add(new DividerItem());
                }
            }
        }
        return flat;
    }

    // ── Date helpers ──────────────────────────────────────────────────────────

    /**
     * Returns a human-readable section label: "Today", "Yesterday",
     * or "Thursday\nDecember 29, 2022" style for older dates.
     */
    private String sectionLabel(long timestampMs) {
        long nowMs = System.currentTimeMillis();
        if (isSameDay(timestampMs, nowMs)) return getString(R.string.history_section_today);
        long yesterday = nowMs - 24L * 60 * 60 * 1000;
        if (isSameDay(timestampMs, yesterday)) return getString(R.string.history_section_yesterday);

        // Older: "Thursday\nDecember 29, 2022"
        Date date = new Date(timestampMs);
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat fullFmt = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        return dayFmt.format(date) + "\n" + fullFmt.format(date);
    }

    private boolean isSameDay(long ms1, long ms2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(ms1);
        c2.setTimeInMillis(ms2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private String formatRowDate(long timestampMs) {
        long nowMs = System.currentTimeMillis();
        Date date = new Date(timestampMs);
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFmt.format(date);

        if (isSameDay(timestampMs, nowMs)) return "Today " + timeStr;
        long yesterday = nowMs - 24L * 60 * 60 * 1000;
        if (isSameDay(timestampMs, yesterday)) return "Yesterday " + timeStr;

        SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return dateFmt.format(date) + " " + timeStr;
    }

    private String formatRelativeWords(long timestampMs) {
        long nowMs = System.currentTimeMillis();
        long diffMs = nowMs - timestampMs;
        long diffDays = diffMs / (24L * 60 * 60 * 1000);
        if (isSameDay(timestampMs, nowMs)) return "Today";
        if (diffDays < 2) return "Yesterday";
        if (diffDays < 7) return diffDays + " days ago";
        if (diffDays < 14) return "1 week ago";
        if (diffDays < 30) return (diffDays / 7) + " weeks ago";
        return (diffDays / 30) + " months ago";
    }

    private String formatAbsoluteDate(long timestampMs) {
        Date date = new Date(timestampMs);
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault());
        return fmt.format(date);
    }

    // ── Transaction detail bottom sheet ──────────────────────────────────────

    private void showTransactionDetail(HomeActivity.Transaction tx) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_transaction_detail, null);
        dialog.setContentView(sheetView);

        // Merchant
        ((ImageView) sheetView.findViewById(R.id.imageDetailMerchantIcon))
                .setImageResource(tx.iconRes);
        ((TextView) sheetView.findViewById(R.id.textDetailMerchantName))
                .setText(tx.merchantName);
        ((TextView) sheetView.findViewById(R.id.textDetailMerchantType))
                .setText(tx.merchantType);

        // Amount
        FrameLayout amountFrame = sheetView.findViewById(R.id.frameDetailAmount);
        TextView amountText = sheetView.findViewById(R.id.textDetailAmount);
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

        // Date
        ((TextView) sheetView.findViewById(R.id.textDetailDateRelative))
                .setText(formatRelativeWords(tx.timestampMs));
        ((TextView) sheetView.findViewById(R.id.textDetailDateAbsolute))
                .setText(formatAbsoluteDate(tx.timestampMs));

        // Tx number
        ((TextView) sheetView.findViewById(R.id.textDetailTxNumber))
                .setText(tx.txNumber);

        // Copy
        sheetView.findViewById(R.id.imageCopyTxNumber).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("tx_number", tx.txNumber));
            Toast.makeText(this, R.string.tx_copied, Toast.LENGTH_SHORT).show();
        });

        // Done
        sheetView.findViewById(R.id.textDetailDone).setOnClickListener(v -> dialog.dismiss());

        // Report
        sheetView.findViewById(R.id.layoutDetailReport).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Report submitted – coming soon", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // ── Dummy data  ───────────────────────────────────────────────────────────

    private List<HomeActivity.Transaction> buildDummyTransactions() {
        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;
        long hour = 60L * 60 * 1000;
        long min = 60L * 1000;

        List<HomeActivity.Transaction> list = new ArrayList<>();

        // Today
        list.add(new HomeActivity.Transaction(
                "Walmart", "Retailer corporation", R.drawable.ic_tx_generic,
                false, 35.23, "PKR", now - (2 * hour + 28 * min), "23010412432431"));
        list.add(new HomeActivity.Transaction(
                "Top up", "Wallet top‑up", R.drawable.ic_topup,
                true, 430.00, "PKR", now - (5 * hour + 48 * min), "23010412432432"));
        list.add(new HomeActivity.Transaction(
                "Netflix", "Subscription", R.drawable.ic_tx_generic,
                false, 13.00, "PKR", now - (8 * hour + 7 * min), "23010412432433"));

        // Yesterday
        list.add(new HomeActivity.Transaction(
                "Amazon", "Online retailer", R.drawable.ic_tx_generic,
                false, 12.23, "PKR", now - (day + 2 * hour + 28 * min), "23010412432434"));
        list.add(new HomeActivity.Transaction(
                "Nike", "Apparel", R.drawable.ic_tx_generic,
                false, 50.23, "PKR", now - (day + 5 * hour + 48 * min), "23010412432435"));
        list.add(new HomeActivity.Transaction(
                "The Home Depot", "Hardware store", R.drawable.ic_tx_generic,
                false, 129.00, "PKR", now - (day + 9 * hour + 7 * min), "23010412432436"));

        // 3 days ago
        list.add(new HomeActivity.Transaction(
                "Amazon", "Online retailer", R.drawable.ic_tx_generic,
                false, 35.23, "PKR", now - (3 * day + 2 * hour + 28 * min), "23010412432437"));
        list.add(new HomeActivity.Transaction(
                "Top up", "Wallet top‑up", R.drawable.ic_topup,
                true, 200.00, "PKR", now - (3 * day + 6 * hour), "23010412432438"));

        // 5 days ago
        list.add(new HomeActivity.Transaction(
                "Spotify", "Music subscription", R.drawable.ic_tx_generic,
                false, 9.99, "PKR", now - (5 * day + hour), "23010412432439"));
        list.add(new HomeActivity.Transaction(
                "Noon", "E-commerce", R.drawable.ic_tx_generic,
                false, 67.50, "PKR", now - (5 * day + 4 * hour), "23010412432440"));

        // 10 days ago
        list.add(new HomeActivity.Transaction(
                "Carrefour", "Supermarket", R.drawable.ic_tx_generic,
                false, 210.75, "PKR", now - (10 * day + 3 * hour), "23010412432441"));
        list.add(new HomeActivity.Transaction(
                "Top up", "Wallet top‑up", R.drawable.ic_topup,
                true, 1000.00, "PKR", now - (10 * day + 7 * hour), "23010412432442"));

        return list;
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────

    private class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<ListItem> items;

        HistoryAdapter(List<ListItem> items) {
            this.items = new ArrayList<>(items);
        }

        void updateList(List<ListItem> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_HEADER) {
                return new HeaderVH(inf.inflate(
                        R.layout.item_history_section_header, parent, false));
            } else if (viewType == VIEW_TYPE_DIVIDER) {
                return new DividerVH(inf.inflate(
                        R.layout.item_history_row_divider, parent, false));
            } else {
                return new TxVH(inf.inflate(
                        R.layout.item_history_transaction, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderVH) {
                HeaderItem item = (HeaderItem) items.get(position);
                ((HeaderVH) holder).bind(item);
            } else if (holder instanceof TxVH) {
                TransactionItem item = (TransactionItem) items.get(position);
                ((TxVH) holder).bind(item.tx);
            }
            // DividerVH needs no binding
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // ── ViewHolders ───────────────────────────────────────────────────────

        class HeaderVH extends RecyclerView.ViewHolder {
            final View divider;
            final TextView label;

            HeaderVH(@NonNull View v) {
                super(v);
                divider = v.findViewById(R.id.viewSectionDivider);
                label = v.findViewById(R.id.textSectionDate);
            }

            void bind(HeaderItem item) {
                divider.setVisibility(item.showTopDivider ? View.VISIBLE : View.GONE);
                label.setText(item.label);
            }
        }

        class TxVH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView date;
            final TextView amount;

            TxVH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.imageTxIcon);
                name = v.findViewById(R.id.textTxName);
                date = v.findViewById(R.id.textTxDate);
                amount = v.findViewById(R.id.textTxAmount);
            }

            void bind(HomeActivity.Transaction tx) {
                icon.setImageResource(tx.iconRes);
                name.setText(tx.merchantName);
                date.setText(formatRowDate(tx.timestampMs));

                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                nf.setMinimumFractionDigits(2);
                nf.setMaximumFractionDigits(2);
                String formatted = nf.format(tx.amount);

                if (tx.isCredit) {
                    amount.setText("+" + tx.currency + " " + formatted);
                    amount.setTextColor(getColor(R.color.tx_credit_text));
                } else {
                    amount.setText("-" + tx.currency + " " + formatted);
                    amount.setTextColor(getColor(R.color.tx_debit_text));
                }

                itemView.setOnClickListener(v -> showTransactionDetail(tx));
            }
        }

        class DividerVH extends RecyclerView.ViewHolder {
            DividerVH(@NonNull View v) { super(v); }
        }
    }

    // ── Static factory ────────────────────────────────────────────────────────

    public static Intent newIntent(Context context) {
        return new Intent(context, HistoryActivity.class);
    }
}