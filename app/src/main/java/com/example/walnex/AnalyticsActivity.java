package com.example.walnex;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.transfer.TransferRecord;
import com.example.walnex.wallet.WalletDefaults;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private static final int LOOKBACK_DAYS = 7;
    private static final int MAX_TX = 200;

    private TextView textUpdatedAt;
    private TextView textTotalIn;
    private TextView textTotalOut;
    private TextView textNetFlow;
    private TextView textTxCount;

    private TextView[] dayLabels;
    private View[] dayBars;

    private TextView[] topNames;
    private TextView[] topAmounts;
    private TextView textTopEmpty;

    private ListenerRegistration statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analytics);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analyticsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRealtimeStats();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (statsListener != null) {
            statsListener.remove();
            statsListener = null;
        }
    }

    private void bindViews() {
        textUpdatedAt = findViewById(R.id.textAnalyticsUpdated);
        textTotalIn = findViewById(R.id.textKpiInValue);
        textTotalOut = findViewById(R.id.textKpiOutValue);
        textNetFlow = findViewById(R.id.textKpiNetValue);
        textTxCount = findViewById(R.id.textKpiCountValue);

        dayBars = new View[] {
            findViewById(R.id.barDay0),
            findViewById(R.id.barDay1),
            findViewById(R.id.barDay2),
            findViewById(R.id.barDay3),
            findViewById(R.id.barDay4),
            findViewById(R.id.barDay5),
            findViewById(R.id.barDay6)
        };

        dayLabels = new TextView[] {
            findViewById(R.id.textDay0),
            findViewById(R.id.textDay1),
            findViewById(R.id.textDay2),
            findViewById(R.id.textDay3),
            findViewById(R.id.textDay4),
            findViewById(R.id.textDay5),
            findViewById(R.id.textDay6)
        };

        topNames = new TextView[] {
            findViewById(R.id.textTopName1),
            findViewById(R.id.textTopName2),
            findViewById(R.id.textTopName3)
        };

        topAmounts = new TextView[] {
            findViewById(R.id.textTopAmount1),
            findViewById(R.id.textTopAmount2),
            findViewById(R.id.textTopAmount3)
        };

        textTopEmpty = findViewById(R.id.textTopEmpty);
    }

    private void startRealtimeStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        Query query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .collection("transactions")
            .orderBy(TransferRecord.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(MAX_TX);

        statsListener = query.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) {
                return;
            }

            String currency = WalletDefaults.DEFAULT_CURRENCY;
            double totalIn = 0;
            double totalOut = 0;
            int txCount = 0;

            List<DayBucket> buckets = buildDayBuckets();
            Map<String, Double> topRecipients = new HashMap<>();

            for (QueryDocumentSnapshot doc : snapshot) {
                Double amount = doc.getDouble(TransferRecord.FIELD_AMOUNT);
                if (amount == null) {
                    continue;
                }

                String docCurrency = doc.getString(TransferRecord.FIELD_CURRENCY);
                if (!TextUtils.isEmpty(docCurrency)) {
                    currency = docCurrency;
                }

                Timestamp ts = doc.getTimestamp(TransferRecord.FIELD_CREATED_AT);
                if (ts == null) {
                    continue;
                }
                long tsMs = ts.toDate().getTime();

                String senderUid = doc.getString(TransferRecord.FIELD_SENDER_UID);
                boolean isCredit = senderUid == null || !senderUid.equals(user.getUid());

                if (isCredit) {
                    totalIn += amount;
                } else {
                    totalOut += amount;
                }
                txCount++;

                addToBucket(buckets, tsMs, Math.abs(amount));

                if (!isCredit) {
                    String name = resolveCounterpartyName(doc, user.getUid());
                    if (TextUtils.isEmpty(name)) {
                        name = getString(R.string.analytics_unknown_recipient);
                    }
                    double existing = topRecipients.containsKey(name)
                        ? topRecipients.get(name)
                        : 0;
                    topRecipients.put(name, existing + amount);
                }
            }

            updateKpis(totalIn, totalOut, txCount, currency);
            updateChart(buckets);
            updateTopRecipients(topRecipients, currency);
            updateUpdatedTime();
        });
    }

    private void updateKpis(double totalIn, double totalOut, int txCount, String currency) {
        double net = totalIn - totalOut;
        textTotalIn.setText(formatAmount(totalIn, currency));
        textTotalOut.setText(formatAmount(totalOut, currency));
        textNetFlow.setText(formatSignedAmount(net, currency));
        textTxCount.setText(String.valueOf(txCount));

        int colorId = net >= 0 ? R.color.tx_credit_text : R.color.tx_debit_text;
        textNetFlow.setTextColor(ContextCompat.getColor(this, colorId));
    }

    private void updateChart(List<DayBucket> buckets) {
        double max = 0;
        for (DayBucket bucket : buckets) {
            if (bucket.total > max) {
                max = bucket.total;
            }
        }

        int minHeight = dpToPx(10);
        int maxHeight = dpToPx(130);

        for (int i = 0; i < buckets.size() && i < dayBars.length; i++) {
            DayBucket bucket = buckets.get(i);
            int height = max <= 0
                ? minHeight
                : minHeight + (int) ((bucket.total / max) * (maxHeight - minHeight));

            View bar = dayBars[i];
            if (bar != null) {
                android.view.ViewGroup.LayoutParams params = bar.getLayoutParams();
                params.height = height;
                bar.setLayoutParams(params);
            }

            TextView label = dayLabels[i];
            if (label != null) {
                label.setText(bucket.label);
            }
        }
    }

    private void updateTopRecipients(Map<String, Double> topRecipients, String currency) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(topRecipients.entrySet());
        Collections.sort(entries, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        boolean hasItems = !entries.isEmpty();
        textTopEmpty.setVisibility(hasItems ? View.GONE : View.VISIBLE);

        for (int i = 0; i < topNames.length; i++) {
            TextView nameView = topNames[i];
            TextView amountView = topAmounts[i];

            if (i < entries.size()) {
                Map.Entry<String, Double> entry = entries.get(i);
                if (nameView != null) nameView.setText(entry.getKey());
                if (amountView != null) amountView.setText(formatAmount(entry.getValue(), currency));
                if (nameView != null) nameView.setVisibility(View.VISIBLE);
                if (amountView != null) amountView.setVisibility(View.VISIBLE);
            } else {
                if (nameView != null) nameView.setVisibility(View.GONE);
                if (amountView != null) amountView.setVisibility(View.GONE);
            }
        }
    }

    private void updateUpdatedTime() {
        if (textUpdatedAt == null) return;
        SimpleDateFormat fmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String time = fmt.format(new Date());
        textUpdatedAt.setText(getString(R.string.analytics_updated_time, time));
    }

    private List<DayBucket> buildDayBuckets() {
        List<DayBucket> buckets = new ArrayList<>();
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEE", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        cal.add(Calendar.DAY_OF_YEAR, -(LOOKBACK_DAYS - 1));

        for (int i = 0; i < LOOKBACK_DAYS; i++) {
            long startMs = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            long endMs = cal.getTimeInMillis();
            String label = labelFmt.format(new Date(startMs));
            buckets.add(new DayBucket(startMs, endMs, label));
        }
        return buckets;
    }

    private void addToBucket(List<DayBucket> buckets, long tsMs, double amount) {
        for (DayBucket bucket : buckets) {
            if (tsMs >= bucket.startMs && tsMs < bucket.endMs) {
                bucket.total += amount;
                return;
            }
        }
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

    private String formatAmount(double amount, String currency) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return currency + " " + nf.format(amount);
    }

    private String formatSignedAmount(double amount, String currency) {
        String formatted = formatAmount(Math.abs(amount), currency);
        return amount >= 0 ? "+" + formatted : "-" + formatted;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class DayBucket {
        final long startMs;
        final long endMs;
        final String label;
        double total;

        DayBucket(long startMs, long endMs, String label) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.label = label;
        }
    }
}