package com.example.walnex.auth;

import android.text.TextUtils;

public final class PhoneNumberFormatter {

    private PhoneNumberFormatter() {
        // Utility class.
    }

    public static String toE164(String countryCode, String rawNumber) {
        if (TextUtils.isEmpty(countryCode) || TextUtils.isEmpty(rawNumber)) {
            return null;
        }

        String digits = rawNumber.replaceAll("[^0-9]", "");
        if (TextUtils.isEmpty(digits)) {
            return null;
        }

        while (digits.startsWith("0") && digits.length() > 1) {
            digits = digits.substring(1);
        }

        if (digits.length() < 7 || digits.length() > 12) {
            return null;
        }

        String normalizedCountryCode = countryCode.startsWith("+") ? countryCode : "+" + countryCode;
        return normalizedCountryCode + digits;
    }

    public static String maskPhone(String phoneE164) {
        if (TextUtils.isEmpty(phoneE164) || phoneE164.length() < 6) {
            return phoneE164;
        }

        int visiblePrefix = Math.min(4, phoneE164.length() - 2);
        int visibleSuffix = 2;
        String prefix = phoneE164.substring(0, visiblePrefix);
        String suffix = phoneE164.substring(phoneE164.length() - visibleSuffix);

        return prefix + "******" + suffix;
    }
}
