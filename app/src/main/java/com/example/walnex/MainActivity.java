package com.example.walnex;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.walnex.startup.LaunchDestination;
import com.example.walnex.startup.MainViewModel;
import com.example.walnex.startup.StartupState;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final String WORDMARK_TEXT = "WalNex";
    private static final long ENTER_ANIMATION_DURATION_MS = 280L;
    private static final long LETTER_REVEAL_DURATION_MS = 740L;
    private static final long FINAL_HOLD_DURATION_MS = 130L;
    private static final long PULSE_UP_DURATION_MS = 110L;
    private static final long PULSE_DOWN_DURATION_MS = 120L;
    private static final long EXIT_FADE_DURATION_MS = 140L;

    private MainViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView splashWordmarkTextView;

    private ValueAnimator letterAnimator;
    private Runnable pendingRouteRunnable;
    private boolean hasRouted;

    private LaunchDestination destination;
    private StartupState startupState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        splashWordmarkTextView = findViewById(R.id.textSplashWordmark);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        startupState = viewModel.resolveStartupState(this);
        boolean isSignedIn = isSignedInWithFirebase();
        destination = viewModel.resolveLaunchDestination(isSignedIn, startupState);

        playSplashAnimationAndRoute();
    }

    private void playSplashAnimationAndRoute() {
        splashWordmarkTextView.setText("");

        splashWordmarkTextView.setAlpha(0f);
        splashWordmarkTextView.setScaleX(0.94f);
        splashWordmarkTextView.setScaleY(0.94f);
        splashWordmarkTextView.setTranslationY(22f);

        splashWordmarkTextView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(ENTER_ANIMATION_DURATION_MS)
            .setInterpolator(new DecelerateInterpolator())
            .start();

        letterAnimator = ValueAnimator.ofInt(0, WORDMARK_TEXT.length());
        letterAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        letterAnimator.setDuration(LETTER_REVEAL_DURATION_MS);
        letterAnimator.addUpdateListener(animation -> {
            int visibleChars = (int) animation.getAnimatedValue();
            splashWordmarkTextView.setText(WORDMARK_TEXT.substring(0, visibleChars));
        });
        letterAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishSplashAndRoute();
            }
        });
        letterAnimator.start();
    }

    private void finishSplashAndRoute() {
        pendingRouteRunnable = this::fadeOutAndRoute;
        handler.postDelayed(pendingRouteRunnable, FINAL_HOLD_DURATION_MS);
    }

    private void fadeOutAndRoute() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        splashWordmarkTextView.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(PULSE_UP_DURATION_MS)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(() -> splashWordmarkTextView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(PULSE_DOWN_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> splashWordmarkTextView.animate()
                    .alpha(0f)
                    .setDuration(EXIT_FADE_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(this::routeSafely)
                    .start())
                .start())
            .start();
    }

    private void routeSafely() {
        if (hasRouted || isFinishing() || isDestroyed()) {
            return;
        }

        hasRouted = true;
        route(destination, startupState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
        splashWordmarkTextView.animate().cancel();

        if (letterAnimator != null) {
            letterAnimator.cancel();
        }
    }

    private boolean isSignedInWithFirebase() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            return auth.getCurrentUser() != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void route(LaunchDestination destination, StartupState startupState) {
        Intent intent;

        switch (destination) {
            case HOME:
                intent = new Intent(this, HomeActivity.class);
                break;
            case STARTUP_BLOCKING:
                intent = StartupStateActivity.newIntent(this, startupState);
                break;
            case WELCOME:
            default:
                intent = new Intent(this, WelcomeActivity.class);
                break;
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}