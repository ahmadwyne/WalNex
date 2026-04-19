package com.example.walnex;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.startup.StartupState;
import com.google.android.material.button.MaterialButton;

public class StartupStateActivity extends AppCompatActivity {

    private static final String EXTRA_STARTUP_STATE = "extra_startup_state";

    public static Intent newIntent(Context context, StartupState startupState) {
        Intent intent = new Intent(context, StartupStateActivity.class);
        intent.putExtra(EXTRA_STARTUP_STATE, startupState.name());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_startup_state);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.startupStateRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        StartupState state = StartupState.fromName(getIntent().getStringExtra(EXTRA_STARTUP_STATE));
        bindState(state);
    }

    private void bindState(StartupState state) {
        MaterialButton actionButton = findViewById(R.id.buttonStartupAction);

        if (state == StartupState.UPDATE_REQUIRED) {
            setTextContent(
                R.id.textStartupTitle,
                R.string.startup_update_required_title,
                R.id.textStartupMessage,
                R.string.startup_update_required_message
            );
            actionButton.setText(R.string.startup_update_required_action);
            actionButton.setOnClickListener(v -> openStoreListing());
            return;
        }

        setTextContent(
            R.id.textStartupTitle,
            R.string.startup_maintenance_title,
            R.id.textStartupMessage,
            R.string.startup_maintenance_message
        );
        actionButton.setText(R.string.startup_maintenance_action);
        actionButton.setOnClickListener(v -> retryStartupFlow());
    }

    private void setTextContent(int titleViewId, int titleTextId, int messageViewId, int messageTextId) {
        ((android.widget.TextView) findViewById(titleViewId)).setText(titleTextId);
        ((android.widget.TextView) findViewById(messageViewId)).setText(messageTextId);
    }

    private void retryStartupFlow() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openStoreListing() {
        String packageName = getPackageName();

        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent webIntent = new Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
        );
        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(marketIntent);
        } catch (ActivityNotFoundException exception) {
            startActivity(webIntent);
        }
    }
}
