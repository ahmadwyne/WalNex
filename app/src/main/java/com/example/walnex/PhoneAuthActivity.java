package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.walnex.auth.Country;
import com.example.walnex.auth.CountryCodeAdapter;
import com.example.walnex.auth.CountryRepository;
import com.example.walnex.auth.PhoneNumberFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    private static final String TAG = "PhoneAuthActivity";
    private static final String DEFAULT_DIAL_CODE = "+962";
    private static final long OTP_REQUEST_TIMEOUT_MS = 25000L;
    private static final boolean FORCE_OTP_ENTRY = true;

    public static Intent newIntent(Context context, String flowMode) {
        Intent intent = new Intent(context, PhoneAuthActivity.class);
        intent.putExtra(AuthFlow.EXTRA_MODE, flowMode);
        return intent;
    }

    private FirebaseAuth auth;
    private Spinner spinnerCountryCode;
    private EditText editPhoneNumber;
    private TextView textAuthModeTitle;
    private TextView textAuthHint;
    private ProgressBar progressSendOtp;
    private MaterialButton buttonContinue;
    private ImageView imageTopIllustration;

    private String flowMode;
    private String pendingPhoneE164;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable requestTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phone_auth);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.phoneAuthRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        flowMode = getIntent().getStringExtra(AuthFlow.EXTRA_MODE);
        if (TextUtils.isEmpty(flowMode)) {
            flowMode = AuthFlow.MODE_REGISTER;
        }

        bindViews();
        configureUiForMode();
        setupCountryCodeSpinner();
        setupCallbacks();

        findViewById(R.id.textBack).setOnClickListener(v -> finish());
        buttonContinue.setOnClickListener(v -> requestOtp());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelRequestTimeout();
    }

    private void bindViews() {
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        textAuthModeTitle = findViewById(R.id.textAuthModeTitle);
        textAuthHint = findViewById(R.id.textAuthHint);
        progressSendOtp = findViewById(R.id.progressSendOtp);
        buttonContinue = findViewById(R.id.buttonContinue);
        imageTopIllustration = findViewById(R.id.imageTopIllustration);
    }

    private void configureUiForMode() {
        if (AuthFlow.isResetMode(flowMode)) {
            textAuthModeTitle.setText(R.string.auth_reset_credentials);
            textAuthHint.setVisibility(View.VISIBLE);
            textAuthHint.setText(R.string.auth_verify_for_reset);
            imageTopIllustration.setImageResource(R.drawable.security);
            return;
        }

        textAuthModeTitle.setText(R.string.auth_enter_mobile_title);
        textAuthHint.setVisibility(View.GONE);
        imageTopIllustration.setImageResource(R.drawable.phone);
    }

    private void setupCountryCodeSpinner() {
        CountryCodeAdapter adapter = new CountryCodeAdapter(this, CountryRepository.all());
        spinnerCountryCode.setAdapter(adapter);

        int defaultIndex = CountryRepository.indexOfDialCode(DEFAULT_DIAL_CODE);
        if (defaultIndex >= 0) {
            spinnerCountryCode.setSelection(defaultIndex);
        }
    }

    private void setupCallbacks() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                if (FORCE_OTP_ENTRY) {
                    return;
                }
                cancelRequestTimeout();
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException error) {
                cancelRequestTimeout();
                setLoading(false);
                Log.w(TAG, "verifyPhoneNumber failed", error);
                Toast.makeText(
                    PhoneAuthActivity.this,
                    describeVerificationError(error),
                    Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                cancelRequestTimeout();
                setLoading(false);
                Toast.makeText(PhoneAuthActivity.this, R.string.auth_otp_sent, Toast.LENGTH_SHORT).show();
                Intent intent = OtpVerificationActivity.newIntent(
                    PhoneAuthActivity.this,
                    flowMode,
                    pendingPhoneE164,
                    verificationId
                );
                startActivity(intent);
            }
        };
    }

    private void requestOtp() {
        Object selected = spinnerCountryCode.getSelectedItem();
        String countryCode = selected instanceof Country
            ? ((Country) selected).dialCode
            : String.valueOf(selected);
        String phoneE164 = PhoneNumberFormatter.toE164(countryCode, editPhoneNumber.getText().toString());

        if (TextUtils.isEmpty(phoneE164)) {
            Toast.makeText(this, R.string.auth_invalid_mobile, Toast.LENGTH_SHORT).show();
            return;
        }

        pendingPhoneE164 = phoneE164;
        setLoading(true);
        startRequestTimeout();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(result -> {
                cancelRequestTimeout();
                setLoading(false);
                if (result.isSuccessful()) {
                    AuthNavigator.routeAfterSignIn(this, flowMode, pendingPhoneE164);
                } else {
                    Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void startRequestTimeout() {
        cancelRequestTimeout();
        requestTimeout = () -> {
            setLoading(false);
            Toast.makeText(this, R.string.auth_phone_verification_timeout, Toast.LENGTH_LONG).show();
        };
        handler.postDelayed(requestTimeout, OTP_REQUEST_TIMEOUT_MS);
    }

    private void cancelRequestTimeout() {
        if (requestTimeout != null) {
            handler.removeCallbacks(requestTimeout);
            requestTimeout = null;
        }
    }

    private void setLoading(boolean isLoading) {
        progressSendOtp.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        buttonContinue.setEnabled(!isLoading);
    }

    private String describeVerificationError(FirebaseException error) {
        String fallback = getString(R.string.auth_phone_verification_failed);
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return fallback + " (invalid phone number format)";
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return fallback + " (too many attempts, try later)";
        }
        if (error instanceof FirebaseAuthMissingActivityForRecaptchaException) {
            return fallback + " (reCAPTCHA could not start)";
        }
        String message = error.getMessage();
        return TextUtils.isEmpty(message) ? fallback : fallback + ": " + message;
    }
}
