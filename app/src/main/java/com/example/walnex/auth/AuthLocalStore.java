package com.example.walnex.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class AuthLocalStore {

    private static final String PREF_NAME = "walnex_auth_secure";

    private static final String KEY_SIGNED_IN = "signed_in";
    private static final String KEY_LAST_LOGIN_AT_MS = "last_login_at_ms";
    private static final String KEY_LAST_PHONE_E164 = "last_phone_e164";
    private static final String KEY_LAST_MODE = "last_mode";

    private AuthLocalStore() {
        // Utility class.
    }

    public static void markSignedIn(Context context, String phoneE164, String mode) {
        SharedPreferences preferences = preferences(context.getApplicationContext());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_SIGNED_IN, true);
        editor.putLong(KEY_LAST_LOGIN_AT_MS, System.currentTimeMillis());

        if (!TextUtils.isEmpty(phoneE164)) {
            editor.putString(KEY_LAST_PHONE_E164, phoneE164);
        }

        if (!TextUtils.isEmpty(mode)) {
            editor.putString(KEY_LAST_MODE, mode);
        }

        editor.apply();
    }

    public static void clear(Context context) {
        preferences(context.getApplicationContext()).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            return EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException exception) {
            // Gracefully fallback when encrypted storage cannot be created on some emulators.
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
}
