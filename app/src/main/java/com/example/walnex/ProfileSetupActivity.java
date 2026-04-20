package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthFlow;
import com.example.walnex.auth.AuthNavigator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    public static Intent newIntent(Context context, String phoneE164) {
        Intent intent = new Intent(context, ProfileSetupActivity.class);
        intent.putExtra(AuthFlow.EXTRA_PHONE_E164, phoneE164);
        return intent;
    }

    private EditText editFullName;
    private EditText editEmail;
    private EditText editPhoneReadOnly;
    private CheckBox checkTerms;

    private String phoneE164;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_setup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileSetupRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        phoneE164 = getIntent().getStringExtra(AuthFlow.EXTRA_PHONE_E164);
        if (TextUtils.isEmpty(phoneE164) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            phoneE164 = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        }

        bindViews();
        bindInitialData();

        findViewById(R.id.textBackProfile).setOnClickListener(v -> finish());

        MaterialButton createAccountButton = findViewById(R.id.buttonCreateAccount);
        createAccountButton.setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        editFullName = findViewById(R.id.editFullName);
        editEmail = findViewById(R.id.editEmail);
        editPhoneReadOnly = findViewById(R.id.editPhoneReadOnly);
        checkTerms = findViewById(R.id.checkTerms);
    }

    private void bindInitialData() {
        if (!TextUtils.isEmpty(phoneE164)) {
            editPhoneReadOnly.setText(phoneE164);
        }
    }

    private void saveProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (TextUtils.isEmpty(uid)) {
            AuthNavigator.openWelcome(this);
            return;
        }

        String fullName = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, R.string.auth_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.auth_email_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkTerms.isChecked()) {
            Toast.makeText(this, R.string.auth_accept_terms, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener(snapshot -> {
                Map<String, Object> payload = new HashMap<>();
                payload.put("fullName", fullName);
                payload.put("updatedAt", FieldValue.serverTimestamp());
                payload.put("lastLoginAt", FieldValue.serverTimestamp());

                if (!snapshot.exists()) {
                    payload.put("createdAt", FieldValue.serverTimestamp());
                }

                if (!TextUtils.isEmpty(phoneE164)) {
                    payload.put("phoneE164", phoneE164);
                }

                if (!TextUtils.isEmpty(email)) {
                    payload.put("email", email);
                }

                firestore.collection("users").document(uid)
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, R.string.auth_profile_saved, Toast.LENGTH_SHORT).show();
                        AuthNavigator.openHome(this);
                    })
                    .addOnFailureListener(error ->
                        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show()
                    );
            })
            .addOnFailureListener(error ->
                Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show()
            );
    }
}
