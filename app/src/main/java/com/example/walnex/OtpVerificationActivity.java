package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthFlow;
import com.example.walnex.auth.AuthNavigator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OtpVerificationActivity extends AppCompatActivity {

    public static Intent newIntent(Context context, String flowMode, String phoneE164, String verificationId) {
        Intent intent = new Intent(context, OtpVerificationActivity.class);
        intent.putExtra(AuthFlow.EXTRA_MODE, flowMode);
        intent.putExtra(AuthFlow.EXTRA_PHONE_E164, phoneE164);
        intent.putExtra(AuthFlow.EXTRA_VERIFICATION_ID, verificationId);
        return intent;
    }

    private static final int OTP_LENGTH = 6;
    private static final long RESEND_WINDOW_MS = 32000L;

    private final StringBuilder otpBuilder = new StringBuilder();

    private FirebaseAuth auth;
    private String flowMode;
    private String phoneE164;
    private String verificationId;

    private TextView textOtpCode;
    private TextView textResendCode;
    private MaterialButton buttonDone;

    private CountDownTimer resendTimer;
    private boolean canResend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.otpRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        flowMode = getIntent().getStringExtra(AuthFlow.EXTRA_MODE);
        phoneE164 = getIntent().getStringExtra(AuthFlow.EXTRA_PHONE_E164);
        verificationId = getIntent().getStringExtra(AuthFlow.EXTRA_VERIFICATION_ID);

        if (TextUtils.isEmpty(verificationId) || TextUtils.isEmpty(phoneE164)) {
            finish();
            return;
        }

        bindViews();
        setupKeypad();

        ((TextView) findViewById(R.id.textOtpTarget)).setText(getString(R.string.auth_otp_target_number, phoneE164));
        findViewById(R.id.textOtpBack).setOnClickListener(v -> finish());
        buttonDone.setOnClickListener(v -> verifyTypedOtp());
        textResendCode.setOnClickListener(v -> {
            if (canResend) {
                resendCode();
            }
        });

        updateOtpUi();
        startResendTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    private void bindViews() {
        textOtpCode = findViewById(R.id.textOtpCode);
        textResendCode = findViewById(R.id.textResendCode);
        buttonDone = findViewById(R.id.buttonDone);
    }

    private void setupKeypad() {
        int[] keyIds = {
            R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
            R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9
        };

        View.OnClickListener digitListener = view -> {
            if (otpBuilder.length() >= OTP_LENGTH) {
                return;
            }

            String digit = String.valueOf(view.getTag());
            otpBuilder.append(digit);
            updateOtpUi();
        };

        for (int id : keyIds) {
            Button key = findViewById(id);
            key.setOnClickListener(digitListener);
        }

        ImageButton backspace = findViewById(R.id.keyBackspace);
        backspace.setOnClickListener(view -> {
            if (otpBuilder.length() == 0) {
                return;
            }
            otpBuilder.deleteCharAt(otpBuilder.length() - 1);
            updateOtpUi();
        });
    }

    private void updateOtpUi() {
        StringBuilder visual = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            if (i == 3) {
                visual.append('-');
            }

            if (i < otpBuilder.length()) {
                visual.append(otpBuilder.charAt(i));
            } else {
                visual.append('X');
            }
        }

        textOtpCode.setText(visual.toString());

        boolean ready = otpBuilder.length() == OTP_LENGTH;
        buttonDone.setEnabled(ready);
        textOtpCode.setTextColor(getColor(ready ? R.color.auth_text_dark : R.color.auth_text_muted));
    }

    private void verifyTypedOtp() {
        if (otpBuilder.length() != OTP_LENGTH) {
            Toast.makeText(this, R.string.auth_invalid_otp, Toast.LENGTH_SHORT).show();
            return;
        }

        buttonDone.setEnabled(false);
        buttonDone.setText(R.string.auth_verifying);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otpBuilder.toString());
        auth.signInWithCredential(credential)
            .addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    AuthNavigator.routeAfterSignIn(this, flowMode, phoneE164);
                } else {
                    buttonDone.setEnabled(true);
                    buttonDone.setText(R.string.auth_done);
                    Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void startResendTimer() {
        canResend = false;
        textResendCode.setEnabled(false);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(RESEND_WINDOW_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                textResendCode.setText(getString(R.string.auth_resend_code) + " " + String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {
                canResend = true;
                textResendCode.setEnabled(true);
                textResendCode.setText(R.string.auth_resend_now);
            }
        };

        resendTimer.start();
    }

    private void resendCode() {
        canResend = false;
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener(result -> {
                            if (result.isSuccessful()) {
                                AuthNavigator.routeAfterSignIn(OtpVerificationActivity.this, flowMode, phoneE164);
                            }
                        });
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(OtpVerificationActivity.this, R.string.auth_phone_verification_failed, Toast.LENGTH_SHORT).show();
                    startResendTimer();
                }

                @Override
                public void onCodeSent(@NonNull String newVerificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = newVerificationId;
                    Toast.makeText(OtpVerificationActivity.this, R.string.auth_otp_sent, Toast.LENGTH_SHORT).show();
                    startResendTimer();
                }
            };

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}
