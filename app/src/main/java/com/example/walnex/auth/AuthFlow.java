package com.example.walnex.auth;

public final class AuthFlow {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_PHONE_E164 = "extra_phone_e164";
    public static final String EXTRA_VERIFICATION_ID = "extra_verification_id";

    public static final String MODE_REGISTER = "register";
    public static final String MODE_SIGN_IN = "sign_in";
    public static final String MODE_RESET = "reset";

    private AuthFlow() {
        // Utility class.
    }

    public static boolean isRegisterMode(String mode) {
        return MODE_REGISTER.equals(mode);
    }

    public static boolean isSignInMode(String mode) {
        return MODE_SIGN_IN.equals(mode);
    }

    public static boolean isResetMode(String mode) {
        return MODE_RESET.equals(mode);
    }
}
