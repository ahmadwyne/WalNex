package com.example.walnex;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthFlow;
import com.example.walnex.auth.AuthLocalStore;
import com.example.walnex.auth.AuthNavigator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindUserStatus();
        setupActions();
    }

    private void bindUserStatus() {
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

        TextView status = findViewById(R.id.textSignedInStatus);
        status.setText(getString(R.string.auth_status_signed_in_as, phone));
    }

    private void setupActions() {
        MaterialButton buttonResetCredentials = findViewById(R.id.buttonResetCredentials);
        MaterialButton buttonSignOut = findViewById(R.id.buttonSignOut);

        buttonResetCredentials.setOnClickListener(v -> {
            startActivity(PhoneAuthActivity.newIntent(this, AuthFlow.MODE_RESET));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        buttonSignOut.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.auth_sign_out)
            .setMessage(R.string.auth_sign_out_confirmation)
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .setPositiveButton(R.string.auth_sign_out, (dialog, which) -> {
                FirebaseAuth.getInstance().signOut();
                AuthLocalStore.clear(this);
                AuthNavigator.openWelcome(this);
            })
            .show();
    }
}
