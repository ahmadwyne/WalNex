package com.example.walnex;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.util.Locale;

public class CardPaymentActivity extends AppCompatActivity {

    // ──────────────────────────────────────────────────────────────────────────
    //  Intent extras
    // ──────────────────────────────────────────────────────────────────────────

    private static final String EXTRA_HOLDER_NAME   = "extra_holder_name";
    private static final String EXTRA_MASKED_NUMBER = "extra_masked_number";
    private static final String EXTRA_BALANCE       = "extra_balance";
    private static final String EXTRA_CURRENCY      = "extra_currency";
    private static final String EXTRA_BG_RES        = "extra_bg_res";

    // ──────────────────────────────────────────────────────────────────────────
    //  State
    // ──────────────────────────────────────────────────────────────────────────

    private String  maskedNumber;
    private boolean numberVisible = true;

    // ──────────────────────────────────────────────────────────────────────────
    //  Factory
    // ──────────────────────────────────────────────────────────────────────────

    public static Intent newIntent(Context context,
                                   String holderName,
                                   String maskedNumber,
                                   double balance,
                                   String currency,
                                   int backgroundRes) {
        Intent intent = new Intent(context, CardPaymentActivity.class);
        intent.putExtra(EXTRA_HOLDER_NAME,   holderName);
        intent.putExtra(EXTRA_MASKED_NUMBER, maskedNumber);
        intent.putExtra(EXTRA_BALANCE,       balance);
        intent.putExtra(EXTRA_CURRENCY,      currency);
        intent.putExtra(EXTRA_BG_RES,        backgroundRes);
        return intent;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_payment);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.cardPaymentRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        bindCardData();
        setupNumberToggle();
        setupBackButton();
        setupQrPayButton();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Card data
    // ──────────────────────────────────────────────────────────────────────────

    private void bindCardData() {
        Intent intent = getIntent();
        String holderName = intent.getStringExtra(EXTRA_HOLDER_NAME);
        maskedNumber      = intent.getStringExtra(EXTRA_MASKED_NUMBER);
        double balance    = intent.getDoubleExtra(EXTRA_BALANCE, 0);
        String currency   = intent.getStringExtra(EXTRA_CURRENCY);
        int    bgRes      = intent.getIntExtra(EXTRA_BG_RES, R.drawable.bg_card_circles);

        ImageView imageBg = findViewById(R.id.imagePayCardBg);
        if (imageBg != null) imageBg.setImageResource(bgRes);

        TextView nameView = findViewById(R.id.textPayCardHolderName);
        if (nameView != null && holderName != null) nameView.setText(holderName);

        TextView numberView = findViewById(R.id.textPayCardNumber);
        if (numberView != null && maskedNumber != null) numberView.setText(maskedNumber);

        TextView balanceView = findViewById(R.id.textPayCardBalance);
        if (balanceView != null) {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            String cur = (currency != null) ? currency : "PKR";
            balanceView.setText(cur + " " + nf.format((long) balance));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Eye toggle – show / hide last-four digits
    // ──────────────────────────────────────────────────────────────────────────

    private void setupNumberToggle() {
        TextView  numberView = findViewById(R.id.textPayCardNumber);
        ImageView btnToggle  = findViewById(R.id.btnTogglePayCardNumber);
        if (btnToggle == null || numberView == null) return;

        btnToggle.setOnClickListener(v -> {
            numberVisible = !numberVisible;
            numberView.setText(numberVisible ? maskedNumber : "•••• ••••");
            btnToggle.setImageResource(
                    numberVisible ? R.drawable.ic_eye : R.drawable.ic_eye_off);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Back button
    // ──────────────────────────────────────────────────────────────────────────

    private void setupBackButton() {
        LinearLayout layoutBack = findViewById(R.id.layoutBack);
        if (layoutBack != null) layoutBack.setOnClickListener(v -> finish());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  QR Pay – open camera / QR scanner via implicit intent
    // ──────────────────────────────────────────────────────────────────────────

    private void setupQrPayButton() {
        LinearLayout btnQrPay = findViewById(R.id.btnQrPay);
        if (btnQrPay != null) btnQrPay.setOnClickListener(v -> openQrScanner());
    }

    private void openQrScanner() {
        // Primary: standard QR scanner intent (ZXing Barcode Scanner and compatible apps)
        try {
            Intent qrIntent = new Intent("com.google.zxing.client.android.SCAN");
            qrIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivity(qrIntent);
            return;
        } catch (ActivityNotFoundException ignored) {}

        // Fallback: open camera via implicit intent
        try {
            startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.qr_no_camera), Toast.LENGTH_SHORT).show();
        }
    }
}
