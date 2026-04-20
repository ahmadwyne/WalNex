package com.example.walnex.auth;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.example.walnex.HomeActivity;
import com.example.walnex.ProfileSetupActivity;
import com.example.walnex.WelcomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public final class AuthNavigator {

    private AuthNavigator() {
        // Utility class.
    }

    public static void routeAfterSignIn(Activity activity, String flowMode, String phoneE164) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openWelcome(activity);
            return;
        }

        String resolvedPhone = !TextUtils.isEmpty(phoneE164) ? phoneE164 : user.getPhoneNumber();
        AuthLocalStore.markSignedIn(activity, resolvedPhone, flowMode);
        bootstrapAccount(resolvedPhone);

        if (AuthFlow.isResetMode(flowMode)) {
            openHome(activity);
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener(snapshot -> {
                if (isProfileComplete(snapshot)) {
                    touchLastLogin(firestore, uid);
                    openHome(activity);
                } else {
                    openProfileSetup(activity, resolvedPhone);
                }
            })
            .addOnFailureListener(error -> {
                openProfileSetup(activity, resolvedPhone);
            });
    }

    private static void bootstrapAccount(String phoneE164) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String uid = user.getUid();

        Map<String, Object> fallbackUserPatch = new HashMap<>();
        fallbackUserPatch.put("updatedAt", FieldValue.serverTimestamp());
        fallbackUserPatch.put("lastLoginAt", FieldValue.serverTimestamp());
        if (!TextUtils.isEmpty(phoneE164)) {
            fallbackUserPatch.put("phoneE164", phoneE164);
        }

        firestore.collection("users").document(uid).set(fallbackUserPatch, SetOptions.merge());

        Map<String, Object> bootstrapPayload = new HashMap<>();
        if (!TextUtils.isEmpty(phoneE164)) {
            bootstrapPayload.put("phoneE164", phoneE164);
        }

        try {
            FirebaseFunctions.getInstance().getHttpsCallable("bootstrapUser").call(bootstrapPayload);
        } catch (RuntimeException ignored) {
            // App still works in local/dev setups where callable functions are not deployed yet.
        }
    }

    private static boolean isProfileComplete(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return false;
        }

        String fullName = snapshot.getString("fullName");
        return !TextUtils.isEmpty(fullName);
    }

    private static void touchLastLogin(FirebaseFirestore firestore, String uid) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("lastLoginAt", FieldValue.serverTimestamp());
        patch.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users").document(uid).set(patch, SetOptions.merge());
    }

    public static void openHome(Activity activity) {
        Intent intent = new Intent(activity, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void openWelcome(Activity activity) {
        Intent intent = new Intent(activity, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void openProfileSetup(Activity activity, String phoneE164) {
        Intent intent = ProfileSetupActivity.newIntent(activity, phoneE164);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
