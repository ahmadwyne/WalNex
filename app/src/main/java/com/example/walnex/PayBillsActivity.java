package com.example.walnex;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walnex.wallet.WalletDefaults;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PayBillsActivity extends AppCompatActivity {

    // ── Category definitions ──────────────────────────────────────────────────

    static final class BillerCategory {
        final String name;
        final String type;
        final int    iconRes;
        final int    bgColor;
        final int    iconTint;

        BillerCategory(String name, String type, int iconRes, int bgColor, int iconTint) {
            this.name     = name;
            this.type     = type;
            this.iconRes  = iconRes;
            this.bgColor  = bgColor;
            this.iconTint = iconTint;
        }
    }

    static final BillerCategory[] CATEGORIES = {
        new BillerCategory("Electricity", "Utility",  R.drawable.ic_electricity,  0xFFE8DDFE, 0xFF643CC2),
        new BillerCategory("Water",       "Utility",  R.drawable.ic_water,        0xFFBEECE6, 0xFF009281),
        new BillerCategory("Phone",       "Telecom",  R.drawable.ic_mobile_phone, 0xFFCCE0FF, 0xFF0064CA),
        new BillerCategory("Internet",    "Telecom",  R.drawable.ic_wifi,         0xFFD8F3DC, 0xFF2D6A4F),
        new BillerCategory("Gas",         "Utility",  R.drawable.ic_gas,          0xFFFFF3CD, 0xFFE85D04),
        new BillerCategory("Rent",        "Housing",  R.drawable.ic_home_bill,    0xFFFFD6DA, 0xFF9B2335),
    };

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Intent newIntent(Context context) {
        return new Intent(context, PayBillsActivity.class);
    }

    // ── Views ─────────────────────────────────────────────────────────────────

    private RecyclerView   recyclerBillers;
    private View           layoutEmptyState;
    private EditText       editSearch;
    private BillerAdapter  adapter;

    private final List<BillerModel> allBillers      = new ArrayList<>();
    private final List<BillerModel> filteredBillers = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pay_bills);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.payBillsRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        recyclerBillers  = findViewById(R.id.recyclerBillers);
        layoutEmptyState = findViewById(R.id.layoutBillersEmpty);
        editSearch       = findViewById(R.id.editBillerSearch);

        adapter = new BillerAdapter(filteredBillers);
        recyclerBillers.setLayoutManager(new LinearLayoutManager(this));
        recyclerBillers.setAdapter(adapter);
        recyclerBillers.setNestedScrollingEnabled(false);

        findViewById(R.id.layoutPayBillsBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNewBiller).setOnClickListener(v -> showCategoryPicker());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterBillers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadBillers();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadBillers() {
        BillerRepository.loadBillers()
                .addOnSuccessListener(snap -> {
                    allBillers.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        BillerModel b = new BillerModel();
                        b.id            = doc.getId();
                        b.category      = doc.getString("category");
                        b.accountNumber = doc.getString("accountNumber");
                        Double amt = doc.getDouble("dueAmount");
                        b.dueAmount = amt != null ? amt : 0;
                        Long due = doc.getLong("dueDateMs");
                        b.dueDateMs = due != null ? due : 0;
                        allBillers.add(b);
                    }
                    filterBillers(editSearch.getText().toString());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load billers.", Toast.LENGTH_SHORT).show());
    }

    private void filterBillers(String query) {
        filteredBillers.clear();
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            filteredBillers.addAll(allBillers);
        } else {
            for (BillerModel b : allBillers) {
                boolean matchCat = b.category      != null && b.category.toLowerCase().contains(q);
                boolean matchAcc = b.accountNumber != null && b.accountNumber.toLowerCase().contains(q);
                if (matchCat || matchAcc) filteredBillers.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        boolean empty = filteredBillers.isEmpty();
        recyclerBillers.setVisibility(empty ? View.GONE  : View.VISIBLE);
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ── Category picker ───────────────────────────────────────────────────────

    private void showCategoryPicker() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_category_picker, null);
        sheet.setContentView(v);
        forceWhiteSheet(sheet);

        LinearLayout container = v.findViewById(R.id.categoryContainer);

        for (BillerCategory cat : CATEGORIES) {
            LinearLayout row = buildCategoryRow(cat);
            row.setOnClickListener(c -> {
                sheet.dismiss();
                showNewBillerForm(cat);
            });
            container.addView(row);
        }

        sheet.show();
    }

    private LinearLayout buildCategoryRow(BillerCategory cat) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(12), 0, dpToPx(12));
        row.setClickable(true);
        row.setFocusable(true);
        int[] attrs = { android.R.attr.selectableItemBackground };
        android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
        row.setBackground(ta.getDrawable(0));
        ta.recycle();

        FrameLayout iconFrame = new FrameLayout(this);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
        iconFrame.setLayoutParams(fp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(cat.bgColor);
        iconFrame.setBackground(bg);

        ImageView icon = new ImageView(this);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dpToPx(22), dpToPx(22));
        ip.gravity = Gravity.CENTER;
        icon.setLayoutParams(ip);
        icon.setImageResource(cat.iconRes);
        icon.setColorFilter(cat.iconTint);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconFrame.addView(icon);

        LinearLayout textGroup = new LinearLayout(this);
        LinearLayout.LayoutParams tgp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        tgp.weight = 1;
        tgp.setMarginStart(dpToPx(14));
        textGroup.setLayoutParams(tgp);
        textGroup.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(cat.name);
        tvName.setTextColor(0xFF1A1A2E);
        tvName.setTextSize(15);
        tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);
        textGroup.addView(tvName);

        TextView tvType = new TextView(this);
        tvType.setText(cat.type);
        tvType.setTextColor(0xFF8A8A9A);
        tvType.setTextSize(13);
        textGroup.addView(tvType);

        ImageView chevron = new ImageView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
        chevron.setLayoutParams(cp);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(0xFFBDBDBD);

        row.addView(iconFrame);
        row.addView(textGroup);
        row.addView(chevron);
        return row;
    }

    // ── New Biller form ───────────────────────────────────────────────────────

    private void showNewBillerForm(BillerCategory cat) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_new_biller, null);

        // Category header
        FrameLayout iconFrame = form.findViewById(R.id.frameCatIcon);
        ImageView   iconView  = form.findViewById(R.id.imageCatIcon);
        applyCategoryIcon(iconFrame, iconView, cat);
        ((TextView) form.findViewById(R.id.textCatName)).setText(cat.name);
        ((TextView) form.findViewById(R.id.textCatType)).setText(cat.type);

        EditText editAccount = form.findViewById(R.id.editDialogAccount);
        EditText editAmount  = form.findViewById(R.id.editDialogAmount);
        TextView textDate    = form.findViewById(R.id.textDialogDate);
        View     dateRow     = form.findViewById(R.id.layoutDialogDate);

        final long[] selectedDateMs = { 0L };
        dateRow.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                cal.set(year, month, day, 0, 0, 0);
                selectedDateMs[0] = cal.getTimeInMillis();
                String label = String.format(Locale.US, "%02d / %02d / %04d", day, month + 1, year);
                textDate.setText(label);
                textDate.setTextColor(0xFF1A1A2E);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        androidx.appcompat.app.AlertDialog dlg =
                new AlertDialog.Builder(this, R.style.WalNex_Dialog_White)
                        .setView(form)
                        .setPositiveButton("Save", (d, w) -> {
                            String account   = editAccount.getText().toString().trim();
                            String amountStr = editAmount.getText().toString().trim();
                            if (TextUtils.isEmpty(account)) {
                                Toast.makeText(this, "Enter account number.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            double amount = 0;
                            if (!TextUtils.isEmpty(amountStr)) {
                                try { amount = Double.parseDouble(amountStr); }
                                catch (NumberFormatException ignored) {}
                            }
                            saveBiller(cat, account, amount, selectedDateMs[0]);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF643CC2);
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF8A8A9A);
    }

    private void saveBiller(BillerCategory cat, String account, double amount, long dueDateMs) {
        BillerRepository.addBiller(cat.name, account, amount, dueDateMs)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, cat.name + " biller saved.", Toast.LENGTH_SHORT).show();
                    loadBillers();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not save biller.", Toast.LENGTH_SHORT).show());
    }

    // ── Biller detail bottom sheet ────────────────────────────────────────────

    private void showBillerDetail(BillerModel biller) {
        BillerCategory cat = getCategoryFor(biller.category);

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_biller_detail, null);
        sheet.setContentView(v);
        forceWhiteSheet(sheet);

        // Bind icon frame
        FrameLayout iconFrame = v.findViewById(R.id.frameDetailIcon);
        ImageView   iconView  = v.findViewById(R.id.imageDetailIcon);
        applyCategoryIcon(iconFrame, iconView, cat);

        // Bind text fields
        ((TextView) v.findViewById(R.id.textDetailName)).setText(biller.category);
        ((TextView) v.findViewById(R.id.textDetailType)).setText(cat.type);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        ((TextView) v.findViewById(R.id.textDetailAmount))
                .setText(WalletDefaults.DEFAULT_CURRENCY + " " + nf.format(biller.dueAmount));

        String dateLabel = biller.dueDateMs > 0 ? formatDate(biller.dueDateMs) : "—";
        ((TextView) v.findViewById(R.id.textDetailDueDate)).setText(dateLabel);
        ((TextView) v.findViewById(R.id.textDetailRegNo)).setText(
                TextUtils.isEmpty(biller.accountNumber) ? "—" : biller.accountNumber);

        v.findViewById(R.id.btnDetailDone).setOnClickListener(c -> sheet.dismiss());

        LinearLayout btnSecure = v.findViewById(R.id.btnSecurePayment);
        btnSecure.setOnClickListener(c -> {
            btnSecure.setEnabled(false);
            submitPayment(biller, sheet);
        });

        sheet.show();
    }

    // ── Payment submission ────────────────────────────────────────────────────

    private void submitPayment(BillerModel biller, BottomSheetDialog sheet) {
        BillerRepository.submitBillPayment(
                biller, biller.dueAmount, WalletDefaults.DEFAULT_CURRENCY)
                .addOnSuccessListener(txId -> {
                    sheet.dismiss();
                    startActivity(PaymentDoneActivity.newIntent(
                            this,
                            biller.category,
                            biller.dueAmount,
                            WalletDefaults.DEFAULT_CURRENCY,
                            txId));
                })
                .addOnFailureListener(e -> {
                    sheet.dismiss();
                    String reason = e instanceof BillerRepository.InsufficientFundsException
                            ? "Insufficient balance to pay this bill."
                            : "Payment failed. Please try again.";
                    startActivity(TransferFailedActivity.newIntent(this, reason));
                });
    }

    // ── Inner adapter ─────────────────────────────────────────────────────────

    private class BillerAdapter extends RecyclerView.Adapter<BillerAdapter.VH> {

        private final List<BillerModel> data;

        BillerAdapter(List<BillerModel> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(PayBillsActivity.this)
                    .inflate(R.layout.item_biller, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BillerModel biller = data.get(pos);
            BillerCategory cat = getCategoryFor(biller.category);

            applyCategoryIcon(h.frameIcon, h.imageIcon, cat);
            h.textCategory.setText(biller.category != null ? biller.category : "");
            h.textAccount.setText(biller.accountNumber != null ? biller.accountNumber : "");

            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            h.textAmount.setText(WalletDefaults.DEFAULT_CURRENCY + " " + nf.format(biller.dueAmount));
            h.textDueDate.setText(biller.dueDateMs > 0 ? formatDate(biller.dueDateMs) : "No due date");

            h.itemView.setOnClickListener(v -> showBillerDetail(biller));
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            final FrameLayout frameIcon;
            final ImageView   imageIcon;
            final TextView    textCategory, textAccount, textAmount, textDueDate;

            VH(View v) {
                super(v);
                frameIcon    = v.findViewById(R.id.frameItemBillerIcon);
                imageIcon    = v.findViewById(R.id.imageItemBillerIcon);
                textCategory = v.findViewById(R.id.textItemBillerCategory);
                textAccount  = v.findViewById(R.id.textItemBillerAccount);
                textAmount   = v.findViewById(R.id.textItemBillerAmount);
                textDueDate  = v.findViewById(R.id.textItemBillerDueDate);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BillerCategory getCategoryFor(String name) {
        if (name != null) {
            for (BillerCategory c : CATEGORIES) {
                if (c.name.equals(name)) return c;
            }
        }
        return CATEGORIES[0];
    }

    private void applyCategoryIcon(FrameLayout frame, ImageView icon, BillerCategory cat) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(cat.bgColor);
        frame.setBackground(bg);
        icon.setImageResource(cat.iconRes);
        icon.setColorFilter(cat.iconTint);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(ms));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /** Forces the Material BottomSheetDialog container to use a plain white background
     *  so it doesn't inherit a dark surface color from the device theme. */
    private static void forceWhiteSheet(BottomSheetDialog sheet) {
        sheet.setOnShowListener(d -> {
            android.widget.FrameLayout bs = ((BottomSheetDialog) d)
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) bs.setBackgroundColor(0xFFFFFFFF);
        });
    }
}
