package com.example.walnex;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.transfer.TransferContact;
// import com.example.walnex.transfer.TransferRepository;  // [FIREBASE DISABLED]
import com.google.android.material.button.MaterialButton;
// import com.google.firebase.auth.FirebaseAuth;            // [FIREBASE DISABLED]

import android.widget.Button;
import android.widget.LinearLayout;

import java.util.UUID;

/**
 * Amount-entry screen shown after the user picks a contact.
 *
 * Firebase / Firestore calls have been commented out and replaced with
 * a local SharedPreferences-backed wallet via {@link WalletManager}.
 *
 * Layout flow:
 *   1. Numeric keypad visible → user enters amount → presses "Done"
 *   2. Keypad slides down, "Secure Payment" button slides up
 *   3. User presses "Secure Payment" → local balance debit via WalletManager
 *   4. On success  → PaymentDoneActivity
 *      On failure  → TransferFailedActivity
 */
public class TransferToUserActivity extends AppCompatActivity {

    private static final String EXTRA_UID    = "extra_recipient_uid";
    private static final String EXTRA_NAME   = "extra_recipient_name";
    private static final String EXTRA_PHONE  = "extra_recipient_phone";
    private static final String EXTRA_AVATAR = "extra_recipient_avatar";

    private static final String CURRENCY = "PKR";

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Intent newIntent(Context context, TransferContact contact) {
        Intent intent = new Intent(context, TransferToUserActivity.class);
        intent.putExtra(EXTRA_UID,    contact.uid);
        intent.putExtra(EXTRA_NAME,   contact.fullName);
        intent.putExtra(EXTRA_PHONE,  contact.phoneE164);
        intent.putExtra(EXTRA_AVATAR, contact.avatarRes);
        return intent;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private TransferContact recipient;

    private TextView       textAmount;
    private LinearLayout   layoutKeypad;
    private MaterialButton btnDone;
    private LinearLayout   layoutSecurePay;

    private final StringBuilder digitBuilder = new StringBuilder();

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer_to_user);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.transferUserRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        extractExtras();
        bindViews();
        bindRecipientCard();
        setupBackButton();
        setupKeypad();
        updateAmountDisplay();

        btnDone.setOnClickListener(v -> onDonePressed());
        layoutSecurePay.setOnClickListener(v -> onSecurePayPressed());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Setup
    // ──────────────────────────────────────────────────────────────────────────

    private void extractExtras() {
        Intent i = getIntent();
        recipient = new TransferContact(
                i.getStringExtra(EXTRA_UID),
                i.getStringExtra(EXTRA_NAME),
                i.getStringExtra(EXTRA_PHONE),
                i.getIntExtra(EXTRA_AVATAR, 0));
    }

    private void bindViews() {
        textAmount      = findViewById(R.id.textTransferAmount);
        layoutKeypad    = findViewById(R.id.layoutNumericKeypad);
        btnDone         = findViewById(R.id.btnTransferDone);
        layoutSecurePay = findViewById(R.id.layoutSecurePayment);
    }

    private void bindRecipientCard() {
        ImageView avatar = findViewById(R.id.imageTransferRecipientAvatar);
        TextView  name   = findViewById(R.id.textTransferRecipientName);
        TextView  phone  = findViewById(R.id.textTransferRecipientPhone);

        name.setText(recipient.fullName);
        phone.setText(recipient.phoneE164);

        if (recipient.avatarRes != 0) {
            avatar.setImageResource(recipient.avatarRes);
        } else {
            avatar.setImageResource(R.drawable.ic_person_avatar);
        }
    }

    private void setupBackButton() {
        findViewById(R.id.layoutTransferUserBack).setOnClickListener(v -> finish());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Numeric keypad
    // ──────────────────────────────────────────────────────────────────────────

    private void setupKeypad() {
        int[] digitIds = {
                R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
                R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9
        };

        View.OnClickListener digitListener = view -> {
            String digit = (String) view.getTag();
            if (digit == null) return;
            if (digitBuilder.length() >= 10) return;
            digitBuilder.append(digit);
            updateAmountDisplay();
        };

        for (int id : digitIds) {
            Button key = findViewById(id);
            if (key != null) key.setOnClickListener(digitListener);
        }

        Button keyDot = findViewById(R.id.keyDot);
        if (keyDot != null) {
            keyDot.setOnClickListener(v -> {
                if (digitBuilder.indexOf(".") == -1) {
                    digitBuilder.append(".");
                    updateAmountDisplay();
                }
            });
        }

        ImageButton keyBackspace = findViewById(R.id.keyBackspace);
        if (keyBackspace != null) {
            keyBackspace.setOnClickListener(v -> {
                if (digitBuilder.length() > 0) {
                    digitBuilder.deleteCharAt(digitBuilder.length() - 1);
                    updateAmountDisplay();
                }
            });
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Amount display
    // ──────────────────────────────────────────────────────────────────────────

    private void updateAmountDisplay() {
        if (digitBuilder.length() == 0) {
            textAmount.setText(CURRENCY + " 00.00");
            textAmount.setTextColor(getColor(R.color.transfer_amount_placeholder));
        } else {
            textAmount.setText(CURRENCY + " " + digitBuilder.toString());
            textAmount.setTextColor(getColor(R.color.transfer_amount_active));
        }
        btnDone.setEnabled(parseAmount() > 0);
    }

    private double parseAmount() {
        try {
            String raw = digitBuilder.toString();
            if (TextUtils.isEmpty(raw) || raw.equals(".")) return 0;
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Done → hide keypad, show Secure Payment button
    // ──────────────────────────────────────────────────────────────────────────

    private void onDonePressed() {
        if (parseAmount() <= 0) {
            Toast.makeText(this, R.string.transfer_enter_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        layoutKeypad.animate()
                .translationY(layoutKeypad.getHeight())
                .alpha(0f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        layoutKeypad.setVisibility(View.GONE);
                        btnDone.setVisibility(View.GONE);
                    }
                }).start();

        layoutSecurePay.setVisibility(View.VISIBLE);
        layoutSecurePay.setAlpha(0f);
        layoutSecurePay.setTranslationY(40f);
        layoutSecurePay.animate()
                .alpha(1f).translationY(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Secure Payment → local SharedPreferences wallet transaction
    //
    //  [FIREBASE DISABLED]
    //  The original Firestore atomic transaction below has been replaced with
    //  a synchronous local debit via WalletManager. Re-enable the Firestore
    //  path by un-commenting TransferRepository.submitTransfer() and removing
    //  the WalletManager block.
    // ──────────────────────────────────────────────────────────────────────────

    private void onSecurePayPressed() {
        double amount = parseAmount();
        if (amount <= 0) return;

        // ── [FIREBASE DISABLED] Original sign-in check ─────────────────────
        // if (FirebaseAuth.getInstance().getCurrentUser() == null) {
        //     Toast.makeText(this, R.string.transfer_not_signed_in, Toast.LENGTH_SHORT).show();
        //     return;
        // }
        // ──────────────────────────────────────────────────────────────────────

        // Prevent double-taps
        layoutSecurePay.setEnabled(false);

        // ── [FIREBASE DISABLED] Original Firestore transaction ─────────────
        // TransferRepository.submitTransfer(recipient, amount, CURRENCY)
        //         .addOnSuccessListener(txId -> { ... })
        //         .addOnFailureListener(e -> { ... });
        // ──────────────────────────────────────────────────────────────────────

        // ── LOCAL WALLET TRANSACTION (SharedPreferences) ───────────────────
        String txId = UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        try {
            // 1. Debit sender balance (throws InsufficientFundsException if broke)
            WalletManager.debitBalance(this, amount);

            // 2. Record as a local transfer transaction
            WalletManager.pushTransaction(this,
                    new WalletManager.LocalTransaction(
                            recipient.fullName,
                            "Transfer",
                            false,               // debit
                            amount,
                            CURRENCY,
                            System.currentTimeMillis(),
                            txId));

            // 3. Update recent-transfers list (shown on Home screen)
            WalletManager.pushRecentTransfer(this,
                    new WalletManager.RecentTransfer(
                            recipient.fullName,
                            recipient.phoneE164,
                            recipient.avatarRes));

            // 4. Navigate to success screen
            Intent successIntent = new Intent(this, PaymentDoneActivity.class);
            successIntent.putExtra("extra_pd_recipient_name", recipient.fullName);
            successIntent.putExtra("extra_pd_amount",         amount);
            successIntent.putExtra("extra_pd_currency",       CURRENCY);
            successIntent.putExtra("extra_pd_tx_id",          txId);
            startActivity(successIntent);
            finish();

        } catch (WalletManager.InsufficientFundsException e) {
            // Re-enable the button so the user can adjust the amount
            layoutSecurePay.setEnabled(true);

            // Navigate to the dedicated failure screen
            Intent failIntent = new Intent(this, TransferFailedActivity.class);
            failIntent.putExtra("extra_tf_reason",
                    "Insufficient balance. Please top up your wallet.");
            startActivity(failIntent);
            // Don't finish — let the user go back and try again

        } catch (Exception e) {
            layoutSecurePay.setEnabled(true);
            Intent failIntent = new Intent(this, TransferFailedActivity.class);
            failIntent.putExtra("extra_tf_reason",
                    "Transfer failed: " + e.getLocalizedMessage());
            startActivity(failIntent);
        }
        // ──────────────────────────────────────────────────────────────────────
    }

    /**
     * Helper to reset the UI if the payment fails,
     * allowing the user to correct the amount.
     */
    private void showKeypadAgain() {
        layoutSecurePay.setVisibility(View.GONE);
        layoutKeypad.setVisibility(View.VISIBLE);
        layoutKeypad.setAlpha(1f);
        layoutKeypad.setTranslationY(0f);
        btnDone.setVisibility(View.VISIBLE);
    }
}