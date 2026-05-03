package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * "Transfer Failed" error screen.
 *
 * Shown when a transfer attempt fails (insufficient funds or unexpected error).
 *
 * Pressing "Back to wallet" navigates back to {@link HomeActivity}.
 */
public class TransferFailedActivity extends AppCompatActivity {

    private static final String EXTRA_REASON = "extra_tf_reason";

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Intent newIntent(Context context, String reason) {
        Intent i = new Intent(context, TransferFailedActivity.class);
        i.putExtra(EXTRA_REASON, reason);
        return i;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer_failed);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.transferFailedRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        TextView reasonView = findViewById(R.id.textTransferFailedReason);
        String reason = getIntent().getStringExtra(EXTRA_REASON);
        if (reasonView != null && !TextUtils.isEmpty(reason)) {
            reasonView.setText(reason);
        }

        // "Back to wallet" clears the stack and goes Home
        Button btnBack = findViewById(R.id.btnBackToWallet);
        btnBack.setOnClickListener(v -> goHome());
    }

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