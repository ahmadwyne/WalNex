package com.example.walnex.startup;

import java.util.Locale;

public enum StartupState {
    OK,
    UPDATE_REQUIRED,
    MAINTENANCE;

    public static StartupState fromName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OK;
        }

        try {
            return StartupState.valueOf(value.trim().toUpperCase(Locale.US));
        } catch (IllegalArgumentException exception) {
            return OK;
        }
    }
}
