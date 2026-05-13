package com.example.walnex;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AddNewCardActivity extends AppCompatActivity {

    private EditText         editHolderName;
    private EditText         editBalance;
    private LinearLayout     btnAddCard;
    private TextView         textBtnAddCard;
    private CardDesignAdapter designAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_card);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.addCardRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        bindViews();
        prefillHolderName();
        setupDesignPicker();
        setupBackButton();
        setupAddCardButton();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        editHolderName  = findViewById(R.id.editHolderName);
        editBalance     = findViewById(R.id.editBalance);
        btnAddCard      = findViewById(R.id.btnAddCard);
        textBtnAddCard  = findViewById(R.id.textBtnAddCard);
    }

    private void prefillHolderName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            editHolderName.setText(user.getDisplayName());
        }
    }

    private void setupDesignPicker() {
        RecyclerView recycler = findViewById(R.id.recyclerCardDesigns);
        designAdapter = new CardDesignAdapter(design -> { /* selection tracked in adapter */ });
        recycler.setAdapter(designAdapter);
        recycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupBackButton() {
        LinearLayout layoutBack = findViewById(R.id.layoutBack);
        if (layoutBack != null) layoutBack.setOnClickListener(v -> finish());
    }

    private void setupAddCardButton() {
        btnAddCard.setOnClickListener(v -> validateAndSave());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Validation & Firestore save
    // ──────────────────────────────────────────────────────────────────────────

    private void validateAndSave() {
        String holderName = editHolderName.getText().toString().trim();
        String lastFour   = String.format(Locale.US, "%04d", new Random().nextInt(10000));
        String balanceStr = editBalance.getText().toString().trim();

        if (TextUtils.isEmpty(holderName)) {
            editHolderName.setError(getString(R.string.add_card_error_name));
            editHolderName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(balanceStr)) {
            editBalance.setError(getString(R.string.add_card_error_balance));
            editBalance.requestFocus();
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
            if (balance < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            editBalance.setError(getString(R.string.add_card_error_balance));
            editBalance.requestFocus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.add_card_error_not_signed_in),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        CardDesignAdapter.CardDesign selected = designAdapter.getSelectedDesign();
        CardModel card = new CardModel(holderName, lastFour, balance, "PKR", selected.type);

        new CardRepository(user.getUid()).addCard(
                card,
                ref -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.add_card_success),
                            Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.add_card_error_save),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void setLoading(boolean loading) {
        btnAddCard.setEnabled(!loading);
        btnAddCard.setAlpha(loading ? 0.6f : 1f);
        textBtnAddCard.setText(loading
                ? getString(R.string.add_card_btn_loading)
                : getString(R.string.add_card_btn));
    }
}