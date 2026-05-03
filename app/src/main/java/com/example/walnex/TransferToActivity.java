package com.example.walnex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walnex.transfer.TransferContact;
import com.example.walnex.transfer.TransferContactsAdapter;
import com.example.walnex.transfer.TransferRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * "Transfer To" contact-picker screen.
 *
 * Displays:
 *  • "New Contact" row  → opens device phonebook to add a new contact
 *  • Search bar         → filters device contacts list in real-time
 *  • Frequent Contacts  → up to 3 most-recent recipients from Firestore
 *  • All Contacts       → contacts from the device's phonebook (READ_CONTACTS)
 *
 * On first launch (or if permission was previously denied) the user is prompted
 * for READ_CONTACTS.  If they deny, the all-contacts section is hidden and a
 * rationale message is shown.
 *
 * Selecting any contact navigates to {@link TransferToUserActivity}.
 */
public class TransferToActivity extends AppCompatActivity {

    // ── Request codes / constants ─────────────────────────────────────────────

    private static final int REQUEST_PICK_CONTACT = 1001;

    // ── Views ─────────────────────────────────────────────────────────────────

    private EditText                editSearch;
    private RecyclerView            rvFrequent;
    private RecyclerView            rvAll;
    private LinearLayout            layoutFrequentSection;
    private LinearLayout            layoutAllSection;
    private TextView                textNoContactsPermission;

    private TransferContactsAdapter frequentAdapter;
    private TransferContactsAdapter allAdapter;

    private List<TransferContact>   allContacts = new ArrayList<>();

    // ── Permission launcher ───────────────────────────────────────────────────

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            loadDeviceContacts();
                        } else {
                            showPermissionDeniedState();
                        }
                    });

    // ── Pick-contact launcher (device phonebook) ──────────────────────────────

    private final ActivityResultLauncher<Void> pickContactLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickContact(),
                    uri -> {
                        if (uri != null) handlePickedContact(uri);
                    });

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer_to);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.transferToRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        bindViews();
        setupBackButton();
        setupNewContact();
        setupSearch();
        setupRecyclerViews();

        // Ask for contacts permission, then load data
        checkAndRequestContactsPermission();

        // Frequent contacts always load from Firestore (no permission needed)
        loadFrequentContacts();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bind
    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        editSearch               = findViewById(R.id.editSearchContact);
        rvFrequent               = findViewById(R.id.rvFrequentContacts);
        rvAll                    = findViewById(R.id.rvAllContacts);
        layoutFrequentSection    = findViewById(R.id.layoutFrequentSection);
        layoutAllSection         = findViewById(R.id.layoutAllSection);
        textNoContactsPermission = findViewById(R.id.textNoContactsPermission);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Back
    // ──────────────────────────────────────────────────────────────────────────

    private void setupBackButton() {
        findViewById(R.id.layoutTransferBack).setOnClickListener(v -> finish());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  New Contact → open device phonebook to pick an existing contact
    // ──────────────────────────────────────────────────────────────────────────

    private void setupNewContact() {
        findViewById(R.id.layoutNewContact).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                // Launch contact picker
                pickContactLauncher.launch(null);
            } else {
                // Permission not yet granted — open system "Add Contact" screen instead
                Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                startActivity(intent);
            }
        });
    }

    /**
     * Handles a contact URI returned by the system contact picker.
     * Reads the display name and first phone number, then navigates to the
     * amount-entry screen.
     */
    private void handlePickedContact(Uri contactUri) {
        TransferContact contact = TransferRepository.readContactFromUri(this, contactUri);
        if (contact == null) {
            Toast.makeText(this, R.string.transfer_contact_read_error, Toast.LENGTH_SHORT).show();
            return;
        }
        openTransferUser(contact);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Permission
    // ──────────────────────────────────────────────────────────────────────────

    private void checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            loadDeviceContacts();
        } else {
            // Request the permission — the launcher callback handles the result
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void showPermissionDeniedState() {
        layoutAllSection.setVisibility(View.VISIBLE);
        rvAll.setVisibility(View.GONE);
        if (textNoContactsPermission != null) {
            textNoContactsPermission.setVisibility(View.VISIBLE);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Search
    // ──────────────────────────────────────────────────────────────────────────

    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence query, int start, int before, int count) {
                filterContacts(query.toString().trim());
            }
        });
    }

    private void filterContacts(String query) {
        if (query.isEmpty()) {
            allAdapter.submitList(allContacts);
            layoutFrequentSection.setVisibility(
                    frequentAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
            return;
        }

        // Hide frequent section during active search
        layoutFrequentSection.setVisibility(View.GONE);

        String lower = query.toLowerCase();
        List<TransferContact> filtered = new ArrayList<>();
        for (TransferContact c : allContacts) {
            if (c.fullName.toLowerCase().contains(lower)
                    || c.phoneE164.contains(query)) {
                filtered.add(c);
            }
        }
        allAdapter.submitList(filtered);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  RecyclerViews
    // ──────────────────────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        frequentAdapter = new TransferContactsAdapter(this::openTransferUser);
        rvFrequent.setLayoutManager(new LinearLayoutManager(this));
        rvFrequent.setAdapter(frequentAdapter);
        rvFrequent.setNestedScrollingEnabled(false);

        allAdapter = new TransferContactsAdapter(this::openTransferUser);
        rvAll.setLayoutManager(new LinearLayoutManager(this));
        rvAll.setAdapter(allAdapter);
        rvAll.setNestedScrollingEnabled(false);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Load contacts
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads device contacts on a background thread and posts the result to
     * the main thread via TransferRepository helper.
     */
    private void loadDeviceContacts() {
        // Hide the "no permission" message if visible
        if (textNoContactsPermission != null) {
            textNoContactsPermission.setVisibility(View.GONE);
        }
        rvAll.setVisibility(View.VISIBLE);

        // Run on background thread — ContentResolver queries block the UI thread
        new Thread(() -> {
            List<TransferContact> contacts =
                    TransferRepository.loadDeviceContacts(this);

            runOnUiThread(() -> {
                allContacts = contacts;
                allAdapter.submitList(allContacts);

                if (allContacts.isEmpty()) {
                    Toast.makeText(this,
                            R.string.transfer_no_device_contacts,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * Loads frequent contacts (Firestore) — independent of device contacts.
     */
    private void loadFrequentContacts() {
        TransferRepository.loadFrequentContacts()
                .addOnSuccessListener(list -> {
                    if (list == null || list.isEmpty()) {
                        layoutFrequentSection.setVisibility(View.GONE);
                    } else {
                        layoutFrequentSection.setVisibility(View.VISIBLE);
                        frequentAdapter.submitList(list);
                    }
                })
                .addOnFailureListener(e -> layoutFrequentSection.setVisibility(View.GONE));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Navigate to amount entry
    // ──────────────────────────────────────────────────────────────────────────

    private void openTransferUser(TransferContact contact) {
        Intent intent = TransferToUserActivity.newIntent(this, contact);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}