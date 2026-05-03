package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthLocalStore;
import com.example.walnex.auth.AuthNavigator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private TextView textSignedInAs;
    private MaterialButton buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textSignedInAs = findViewById(R.id.textSignedInAs);
        buttonLogout = findViewById(R.id.buttonLogout);

        findViewById(R.id.layoutSettingsBack).setOnClickListener(v -> finish());
        buttonLogout.setOnClickListener(v -> confirmLogout());

        bindSignedInStatus();
    }

    private void bindSignedInStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AuthLocalStore.clear(this);
            AuthNavigator.openWelcome(this);
            return;
        }

        String phone = user.getPhoneNumber();
        if (TextUtils.isEmpty(phone)) {
            phone = user.getUid();
        }

        textSignedInAs.setText(getString(R.string.auth_status_signed_in_as, phone));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.auth_sign_out)
            .setMessage(R.string.auth_sign_out_confirmation)
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .setPositiveButton(R.string.auth_sign_out, (dialog, which) -> signOut())
            .show();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        AuthLocalStore.clear(this);
        AuthNavigator.openWelcome(this);
    }
}
