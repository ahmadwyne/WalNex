package com.example.walnex;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthFlow;
import com.example.walnex.ui.BrandingUtils;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcomeRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView welcomeWordmark = findViewById(R.id.textWelcomeWordmark);
        welcomeWordmark.setText(BrandingUtils.signatureWordmark(this, getString(R.string.walnex_wordmark)));
        animateWordmark(welcomeWordmark);

        MaterialButton getStartedButton = findViewById(R.id.buttonGetStarted);
        getStartedButton.setOnClickListener(v -> openPhoneAuth(AuthFlow.MODE_REGISTER));

        findViewById(R.id.buttonExistingAccount).setOnClickListener(v -> openPhoneAuth(AuthFlow.MODE_SIGN_IN));
    }

    private void animateWordmark(TextView wordmark) {
        wordmark.setScaleX(0.93f);
        wordmark.setScaleY(0.93f);
        wordmark.setAlpha(0f);

        wordmark.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(460L)
            .setStartDelay(80L)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    private void openPhoneAuth(String mode) {
        Intent intent = PhoneAuthActivity.newIntent(this, mode);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
