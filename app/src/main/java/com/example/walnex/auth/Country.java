package com.example.walnex.auth;

import android.text.TextUtils;

public final class Country {

    public final String name;
    public final String iso2;
    public final String dialCode;

    public Country(String name, String iso2, String dialCode) {
        this.name = name;
        this.iso2 = iso2;
        this.dialCode = dialCode;
    }

    public String flagEmoji() {
        if (TextUtils.isEmpty(iso2) || iso2.length() != 2) {
            return "";
        }
        int base = 0x1F1E6;
        int first = base + (Character.toUpperCase(iso2.charAt(0)) - 'A');
        int second = base + (Character.toUpperCase(iso2.charAt(1)) - 'A');
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    @Override
    public String toString() {
        return dialCode;
    }
}
