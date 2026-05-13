package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.walnex.auth.AuthLocalStore;
import com.example.walnex.auth.AuthNavigator;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final int[] AVATAR_RES = {
            R.drawable.ic_avatar_1,
            R.drawable.ic_avatar_2,
            R.drawable.ic_avatar_3
    };

    // ── Factory ───────────────────────────────────────────────────────────────
    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    // ── Views ─────────────────────────────────────────────────────────────────
    private ImageView imageAvatar;
    private TextView  textName;
    private TextView  textJoined;
    private TextView  textFullNameValue;
    private TextView  textPhoneValue;

    // ── State ─────────────────────────────────────────────────────────────────
    private int    currentAvatarIndex = -1; // -1 = gallery/default
    private String currentFullName    = "";
    private String currentPhone       = "";

    // ── Gallery picker ────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        saveGalleryToLocalFile(uri);
                    });

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.settingsRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        imageAvatar       = findViewById(R.id.imageSettingsAvatar);
        imageAvatar.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        imageAvatar.setClipToOutline(true);
        textName          = findViewById(R.id.textSettingsName);
        textJoined        = findViewById(R.id.textSettingsJoinedDate);
        textFullNameValue = findViewById(R.id.textSettingsFullNameValue);
        textPhoneValue    = findViewById(R.id.textSettingsPhoneValue);

        findViewById(R.id.layoutSettingsBack)
                .setOnClickListener(v -> finish());
        findViewById(R.id.frameSettingsAvatar)
                .setOnClickListener(v -> showAvatarPicker());

        setupEditButtons();
        setupSignOut();
        loadProfile();
    }

    // ── Edit buttons ──────────────────────────────────────────────────────────
    private void setupEditButtons() {
        findViewById(R.id.textEditFullName).setOnClickListener(v ->
                showEditDialog(
                        "Full Name", currentFullName,
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                        newVal -> {
                            currentFullName = newVal;
                            textFullNameValue.setText(newVal);
                            textName.setText(newVal);
                            saveField("fullName", newVal);
                        }));

        findViewById(R.id.textEditPhone).setOnClickListener(v ->
                showEditDialog(
                        "Phone Number", currentPhone,
                        InputType.TYPE_CLASS_PHONE,
                        newVal -> {
                            currentPhone = newVal;
                            textPhoneValue.setText(newVal);
                            saveField("phoneE164", newVal);
                        }));
    }

    // ── Sign out ──────────────────────────────────────────────────────────────
    private void setupSignOut() {
        findViewById(R.id.layoutSettingsSignOut).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.auth_sign_out)
                        .setMessage(R.string.auth_sign_out_confirmation)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.auth_sign_out, (d, w) -> {
                            FirebaseAuth.getInstance().signOut();
                            AuthLocalStore.clear(this);
                            AuthNavigator.openWelcome(this);
                        })
                        .show());
    }

    // ── Load profile from Firestore ───────────────────────────────────────────
    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AuthLocalStore.clear(this);
            AuthNavigator.openWelcome(this);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    currentFullName = orEmpty(snap.getString("fullName"));
                    currentPhone    = orEmpty(snap.getString("phoneE164"));

                    textName.setText(currentFullName.isEmpty() ? "User" : currentFullName);
                    textFullNameValue.setText(currentFullName.isEmpty() ? "—" : currentFullName);
                    textPhoneValue.setText(currentPhone.isEmpty() ? "—" : currentPhone);

                    // Joined date (Firestore createdAt or Firebase Auth metadata fallback)
                    com.google.firebase.Timestamp ts = snap.getTimestamp("createdAt");
                    long createdMs = ts != null ? ts.toDate().getTime()
                            : (user.getMetadata() != null
                            ? user.getMetadata().getCreationTimestamp() : 0L);
                    if (createdMs > 0) textJoined.setText(formatJoined(createdMs));

                    // Avatar
                    String avatarUri = snap.getString("avatarUri");
                    Long   avatarIdx = snap.getLong("avatarIndex");

                    if ("local".equals(avatarUri)) {
                        applyLocalGalleryAvatar();
                    } else if (avatarIdx != null && avatarIdx >= 0
                            && avatarIdx < AVATAR_RES.length) {
                        currentAvatarIndex = avatarIdx.intValue();
                        applyPresetAvatar(currentAvatarIndex);
                    }
                })
                .addOnFailureListener(e -> {
                    String name = user.getDisplayName();
                    if (!TextUtils.isEmpty(name)) {
                        currentFullName = name;
                        textName.setText(name);
                        textFullNameValue.setText(name);
                    }
                    String phone = user.getPhoneNumber();
                    if (!TextUtils.isEmpty(phone)) {
                        currentPhone = phone;
                        textPhoneValue.setText(phone);
                    }
                });
    }

    // ── Avatar display ────────────────────────────────────────────────────────
    private void applyPresetAvatar(int index) {
        imageAvatar.setBackground(null);
        imageAvatar.clearColorFilter();
        imageAvatar.setPadding(0, 0, 0, 0);
        imageAvatar.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        imageAvatar.setImageResource(AVATAR_RES[index]);
    }

    private void applyLocalGalleryAvatar() {
        java.io.File f = new java.io.File(getFilesDir(), "user_avatar.jpg");
        if (!f.exists()) return;
        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
        if (bmp == null) return;
        imageAvatar.setBackground(null);
        imageAvatar.clearColorFilter();
        imageAvatar.setPadding(0, 0, 0, 0);
        imageAvatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        imageAvatar.setImageBitmap(bmp);
    }

    private void saveGalleryToLocalFile(Uri sourceUri) {
        new Thread(() -> {
            try (java.io.InputStream in = getContentResolver().openInputStream(sourceUri)) {
                android.graphics.Bitmap src = android.graphics.BitmapFactory.decodeStream(in);
                if (src == null) throw new java.io.IOException("decode failed");
                int size = Math.min(src.getWidth(), src.getHeight());
                int ox = (src.getWidth() - size) / 2;
                int oy = (src.getHeight() - size) / 2;
                android.graphics.Bitmap cropped = android.graphics.Bitmap.createBitmap(src, ox, oy, size, size);
                android.graphics.Bitmap scaled  = android.graphics.Bitmap.createScaledBitmap(cropped, 256, 256, true);
                src.recycle();
                if (src != cropped) cropped.recycle();
                java.io.File dest = new java.io.File(getFilesDir(), "user_avatar.jpg");
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out);
                }
                scaled.recycle();
                runOnUiThread(() -> {
                    currentAvatarIndex = -1;
                    applyLocalGalleryAvatar();
                    saveAvatarToFirestore(-1, "local");
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Avatar picker bottom sheet ────────────────────────────────────────────
    private void showAvatarPicker() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_avatar_picker, null);
        sheet.setContentView(v);

        LinearLayout row1 = v.findViewById(R.id.avatarRow1);

        for (int i = 0; i < AVATAR_RES.length; i++) {
            final int idx = i;

            FrameLayout cell = new FrameLayout(this);
            int size = dpToPx(72);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, size);
            lp.weight = 1;
            lp.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            cell.setLayoutParams(lp);

            if (i == currentAvatarIndex) {
                GradientDrawable sel = new GradientDrawable();
                sel.setShape(GradientDrawable.OVAL);
                sel.setColor(0x00000000);
                sel.setStroke(dpToPx(3), 0xFF643CC2);
                cell.setBackground(sel);
            }

            ImageView icon = new ImageView(this);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dpToPx(64), dpToPx(64));
            ip.gravity = Gravity.CENTER;
            icon.setLayoutParams(ip);
            icon.setImageResource(AVATAR_RES[i]);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View v, android.graphics.Outline outline) {
                    outline.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            icon.setClipToOutline(true);
            cell.addView(icon);

            cell.setClickable(true);
            cell.setFocusable(true);
            cell.setOnClickListener(c -> {
                currentAvatarIndex = idx;
                applyPresetAvatar(idx);
                saveAvatarToFirestore(idx, null);
                sheet.dismiss();
            });

            row1.addView(cell);
        }

        v.findViewById(R.id.layoutPickFromGallery).setOnClickListener(c -> {
            sheet.dismiss();
            pickImageLauncher.launch("image/*");
        });

        sheet.show();
    }

    // ── Edit field dialog ─────────────────────────────────────────────────────
    private void showEditDialog(String label, String current,
                                int inputType, OnFieldSaved cb) {
        EditText editText = new EditText(this);
        editText.setText(current);
        editText.setInputType(inputType);
        editText.setSelection(editText.getText().length());

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        container.setPadding(pad, dpToPx(8), pad, 0);
        container.addView(editText);

        new AlertDialog.Builder(this)
                .setTitle("Edit " + label)
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String val = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(val)) cb.onSaved(val);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Firestore saves ───────────────────────────────────────────────────────
    private void saveField(String field, String value) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put(field, value);
        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(a ->
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not save. Please try again.",
                                Toast.LENGTH_SHORT).show());
    }

    private void saveAvatarToFirestore(int index, String uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("avatarIndex", index);
        data.put("avatarUri",   uri != null ? uri : "");
        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not save avatar.",
                                Toast.LENGTH_SHORT).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String formatJoined(long ms) {
        long days = (System.currentTimeMillis() - ms) / 86_400_000L;
        if (days < 1)  return "Joined today";
        if (days < 30) return "Joined " + days + " days ago";
        long months = days / 30;
        if (months < 12) return months == 1 ? "Joined 1 month ago"
                                             : "Joined " + months + " months ago";
        long years = months / 12;
        return years == 1 ? "Joined 1 year ago" : "Joined " + years + " years ago";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static String orEmpty(String s) { return s != null ? s : ""; }

    interface OnFieldSaved { void onSaved(String newValue); }
}
