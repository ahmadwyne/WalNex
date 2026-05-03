package com.example.walnex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * "Payment Done" success screen.
 *
 * Receives transfer details via Intent extras and displays them using the
 * layout defined in {@code activity_payment_done.xml}.
 *
 * Pressing "Back to wallet" clears the activity back-stack and returns the
 * user to {@link HomeActivity}, where the updated balance and the new entry
 * in the recent-transfers row are immediately visible.
 */
public class PaymentDoneActivity extends AppCompatActivity {

    private static final String EXTRA_RECIPIENT_NAME = "extra_pd_recipient_name";
    private static final String EXTRA_AMOUNT         = "extra_pd_amount";
    private static final String EXTRA_CURRENCY       = "extra_pd_currency";
    private static final String EXTRA_TX_ID          = "extra_pd_tx_id";

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Intent newIntent(Context context,
                                   String recipientName,
                                   double amount,
                                   String currency,
                                   String txId) {
        Intent i = new Intent(context, PaymentDoneActivity.class);
        i.putExtra(EXTRA_RECIPIENT_NAME, recipientName);
        i.putExtra(EXTRA_AMOUNT,         amount);
        i.putExtra(EXTRA_CURRENCY,       currency);
        i.putExtra(EXTRA_TX_ID,          txId);
        return i;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_done);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.paymentDoneRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // Extract extras
        String recipientName = getIntent().getStringExtra(EXTRA_RECIPIENT_NAME);
        double amount        = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0);
        String currency      = getIntent().getStringExtra(EXTRA_CURRENCY);
        String txId          = getIntent().getStringExtra(EXTRA_TX_ID);

        // ── Bind views ────────────────────────────────────────────────────────

        // "Biller" field → recipient name
        TextView tvBiller = findViewById(R.id.tvBiller);
        if (tvBiller != null && recipientName != null) {
            tvBiller.setText(recipientName);
        }

        // Amount
        TextView tvAmount = findViewById(R.id.tvAmount);
        if (tvAmount != null) {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            String curr = currency != null ? currency : "PKR";
            tvAmount.setText(curr + " " + nf.format(amount));
        }

        // Transaction number
        TextView tvTxNo = findViewById(R.id.tvTransactionNo);
        if (tvTxNo != null && txId != null) {
            tvTxNo.setText(txId);
        }

        // Copy button
        ImageView btnCopy = findViewById(R.id.btnCopy);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                if (txId == null) return;
                ClipboardManager cm =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("tx_id", txId));
                Toast.makeText(this, "Transaction ID copied", Toast.LENGTH_SHORT).show();
            });
        }

        // Report a problem
        findViewById(R.id.btnReportProblem).setOnClickListener(v ->
                Toast.makeText(this, "Report submitted – coming soon", Toast.LENGTH_SHORT).show());

        // Back to wallet → clear stack, go Home
        Button btnBack = findViewById(R.id.btnBackToWallet);
        btnBack.setOnClickListener(v -> goHome());
    }

    /** Override hardware back so it also goes cleanly to Home. */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goHome();
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}