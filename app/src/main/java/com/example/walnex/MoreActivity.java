package com.example.walnex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MoreActivity extends AppCompatActivity {

    // ──────────────────────────────────────────────────────────────────────────
    //  Views
    // ──────────────────────────────────────────────────────────────────────────

    // Section 1 – Main options
    private LinearLayout rowPayBills;
    private LinearLayout rowTransfer;
    private LinearLayout rowTopup;
    private LinearLayout rowWithdraw;
    private LinearLayout rowAnalytics;

    // Section 2 – Support
    private LinearLayout rowHelp;
    private LinearLayout rowContactUs;
    private LinearLayout rowAbout;

    // Bottom nav tabs
    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navCards;
    private LinearLayout navMore;

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        // Edge-to-edge insets (matches HomeActivity pattern)
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.moreRoot),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                    return insets;
                });

        bindViews();
        setupRowListeners();
        setupBottomNav();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bind
    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        rowPayBills  = findViewById(R.id.rowPayBills);
        rowTransfer  = findViewById(R.id.rowTransfer);
        rowTopup     = findViewById(R.id.rowTopup);
        rowWithdraw  = findViewById(R.id.rowWithdraw);
        rowAnalytics = findViewById(R.id.rowAnalytics);

        rowHelp      = findViewById(R.id.rowHelp);
        rowContactUs = findViewById(R.id.rowContactUs);
        rowAbout     = findViewById(R.id.rowAbout);

        navHome    = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navCards   = findViewById(R.id.navCards);
        navMore    = findViewById(R.id.navMore);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Row click listeners
    // ──────────────────────────────────────────────────────────────────────────

    private void setupRowListeners() {
        rowPayBills.setOnClickListener(v -> openScreen(PayBillsActivity.class));
        rowTransfer.setOnClickListener(v -> openScreen(TransferActivity.class));
        rowTopup.setOnClickListener(v -> openScreen(TopupActivity.class));
        rowWithdraw.setOnClickListener(v -> openScreen(WithdrawActivity.class));
        rowAnalytics.setOnClickListener(v -> openScreen(AnalyticsActivity.class));

        rowHelp.setOnClickListener(v -> openScreen(HelpActivity.class));
        rowContactUs.setOnClickListener(v -> openScreen(ContactUsActivity.class));
        rowAbout.setOnClickListener(v -> openScreen(AboutActivity.class));
    }

    /**
     * Opens a destination activity without finishing this one so the user
     * can press Back and return to the More screen.
     */
    private void openScreen(Class<?> destination) {
        startActivity(new Intent(this, destination));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bottom navigation
    // ──────────────────────────────────────────────────────────────────────────

    private void setupBottomNav() {

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navCards.setOnClickListener(v -> {
            startActivity(new Intent(this, CardsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navMore.setOnClickListener(v -> { });
    }
}