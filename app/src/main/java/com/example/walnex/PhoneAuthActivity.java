package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.example.walnex.auth.PhoneNumberFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this,
            R.array.country_codes,
            R.layout.item_country_code_selected
        );
        adapter.setDropDownViewResource(R.layout.item_country_code_dropdown);
        spinnerCountryCode.setAdapter(adapter);

        for (int i = 0; i < adapter.getCount(); i++) {
            if ("+962".contentEquals(adapter.getItem(i))) {
                spinnerCountryCode.setSelection(i);
                break;
            }
        }
    }

    private void setupCallbacks() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException error) {
                setLoading(false);
                Toast.makeText(PhoneAuthActivity.this, R.string.auth_phone_verification_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
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
        String countryCode = String.valueOf(spinnerCountryCode.getSelectedItem());
        String phoneE164 = PhoneNumberFormatter.toE164(countryCode, editPhoneNumber.getText().toString());

        if (TextUtils.isEmpty(phoneE164)) {
            Toast.makeText(this, R.string.auth_invalid_mobile, Toast.LENGTH_SHORT).show();
            return;
        }

        pendingPhoneE164 = phoneE164;
        setLoading(true);

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
                setLoading(false);
                if (result.isSuccessful()) {
                    AuthNavigator.routeAfterSignIn(this, flowMode, pendingPhoneE164);
                } else {
                    Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setLoading(boolean isLoading) {
        progressSendOtp.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        buttonContinue.setEnabled(!isLoading);
    }
}
